package com.socket.server.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.core.constant.ChatConstants;
import com.socket.core.constant.ChatProperties;
import com.socket.core.constant.Constants;
import com.socket.core.custom.TokenUserManager;
import com.socket.core.mapper.SysUserMapper;
import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.model.condition.EmailCondition;
import com.socket.core.model.condition.LoginCondition;
import com.socket.core.model.condition.PasswordCondition;
import com.socket.core.model.condition.RegisterCondition;
import com.socket.core.model.enums.FileType;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.enums.UserRole;
import com.socket.core.model.po.ChatRecordFile;
import com.socket.core.model.po.SysUser;
import com.socket.core.util.Bcrypt;
import com.socket.core.util.RedisClient;
import com.socket.core.util.Wss;
import com.socket.secure.util.AES;
import com.socket.secure.util.Assert;
import com.socket.server.custom.publisher.CommandPublisher;
import com.socket.server.custom.storage.ResourceStorage;
import com.socket.server.exception.AccountException;
import com.socket.server.exception.UploadException;
import com.socket.server.service.ResourceService;
import com.socket.server.service.SysGroupService;
import com.socket.server.service.SysUserService;
import com.socket.server.util.Email;
import com.socket.server.util.ShiroUser;
import com.socket.server.util.servlet.Request;
import com.socket.server.util.servlet.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysGroupService sysGroupService;
    private final ResourceService resourceService;
    private final RedisClient<Object> redisClient;
    private final TokenUserManager tokenUserManager;
    private final CommandPublisher publisher;
    private final ChatProperties properties;
    private final ChatConstants constants;
    private final ResourceStorage storage;
    private final HttpSession session;
    private final Email sender;

    @Override
    public void login(LoginCondition condition) {
        String code = condition.getCode();
        String guid = condition.getUser();
        // ??????????????????
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Validator.isEmail(guid) ? SysUser::getEmail : SysUser::getGuid, guid);
        SysUser user = getFirst(wrapper);
        Assert.notNull(user, "?????????????????????", UnknownAccountException::new);
        // ??????????????????
        if (StrUtil.isNotEmpty(code)) {
            String key = RedisTree.EMAIL.concat(user.getEmail());
            Object redisCode = redisClient.get(key);
            Assert.equals(redisCode, code, "??????????????????", AccountException::new);
            Request.set(Constants.AUHT_OFFSITE_REQUEST);
            redisClient.remove(key);
        }
        // shiro??????
        SecurityUtils.getSubject().login(new UsernamePasswordToken(user.getGuid(), condition.getPass(), condition.isAuto()));
        // ????????????????????????
        String token = tokenUserManager.setToken(user.getGuid(), AES.getAesKey(session), session.getMaxInactiveInterval());
        session.setAttribute(Constants.AUTH_TOKEN, token);
        Response.setHeader(Constants.AUTH_TOKEN, token);
    }

    @Override
    public void register(RegisterCondition condition) {
        // ????????????
        String key = RedisTree.EMAIL.concat(condition.getEmail());
        Assert.equals(condition.getCode(), redisClient.get(key), "??????????????????", IllegalStateException::new);
        redisClient.remove(key);
        // ??????
        SysUser user = _register(condition);
        // ??????
        this.login(new LoginCondition(user.getGuid(), condition.getPass()));
    }

    public SysUser _register(RegisterCondition condition) {
        SysUser user = SysUser.buildNewUser();
        user.setName(StrUtil.emptyToDefault(condition.getName(), "??????" + user.getGuid()));
        user.setHeadimgurl(condition.getImgurl());
        user.setEmail(condition.getEmail());
        user.setHash(Bcrypt.digest(condition.getPass()));
        user.setUin(condition.getUin());
        super.save(user);
        // ??????????????????
        sysGroupService.joinGroup(constants.getDefaultGroup(), user.getGuid());
        return user;
    }

    @Override
    public String sendEmail(String email) {
        // ???????????????UID
        if (!Validator.isEmail(email)) {
            final String guid = email;
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getGuid, guid);
            SysUser user = getFirst(wrapper);
            Assert.notNull(user, "???????????????????????????", IllegalStateException::new);
            Assert.isFalse(user.isDeleted(), "?????????????????????????????????", IllegalStateException::new);
            email = user.getEmail();
            Assert.notEmpty(email, "??????????????????????????????", IllegalStateException::new);
        }
        // ????????????????????????
        String etk = RedisTree.EMAIL_TEMP.concat(email);
        Assert.isFalse(redisClient.exist(etk), "???????????????????????????", IllegalStateException::new);
        redisClient.set(etk, -1, properties.getEmailSendingInterval());
        // ????????????????????????
        String elk = RedisTree.EMAIL_LIMIT.concat(email);
        long count = redisClient.incr(elk, 1, properties.getEmailLimitSendingInterval(), TimeUnit.HOURS);
        Assert.isTrue(count <= 3, "????????????????????????????????????????????????", IllegalStateException::new);
        // ????????????
        String code = sender.send(email);
        // ?????????redis 10??????
        etk = RedisTree.EMAIL.concat(email);
        redisClient.set(etk, code, properties.getEmailCodeValidTime(), TimeUnit.MINUTES);
        return DesensitizedUtil.email(email);
    }

    @Override
    public boolean updatePassword(PasswordCondition condition) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        String email = condition.getEmail();
        SysUser user = ShiroUser.get();
        if (StrUtil.isEmpty(email)) {
            Assert.notNull(user, "???????????????", IllegalStateException::new);
            email = user.getEmail();
        }
        String key = RedisTree.EMAIL.concat(email);
        String code = redisClient.get(key);
        Assert.equals(code, condition.getCode(), "????????????????????????", IllegalStateException::new);
        // ????????????????????????
        wrapper.eq(SysUser::getEmail, email);
        wrapper.set(SysUser::getHash, Bcrypt.digest(condition.getPassword()));
        redisClient.remove(key);
        return super.update(wrapper);
    }

    @Override
    public void updateMaterial(SysUser user) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        // ???????????????????????????
        user.setGuid(null);
        user.setHeadimgurl(null);
        user.setEmail(null);
        // ?????????????????? ?????????????????????????????????
        if (user.getBirth() != null) {
            LocalDateTime time = user.getBirth().atStartOfDay();
            long between = LocalDateTimeUtil.between(time, LocalDateTime.now(), ChronoUnit.YEARS);
            user.setAge(Math.toIntExact(between));
        }
        wrapper.eq(SysUser::getGuid, ShiroUser.getUserId());
        Assert.isTrue(super.update(user, wrapper), "????????????", IllegalStateException::new);
        // ??????????????????
        publisher.pushUserEvent(user.getName(), UserEnum.NAME);
    }

    @Override
    public String updateAvatar(byte[] bytes) {
        Assert.isTrue(bytes.length <= 0x4b000, "????????????????????????", UploadException::new);
        // byte[]?????????
        BufferedImage image;
        try {
            image = ImgUtil.toImage(bytes);
        } catch (IllegalArgumentException | IORuntimeException e) {
            throw new UploadException("???????????????????????????");
        }
        // ??????????????????
        int min = Math.min(image.getWidth(), image.getHeight());
        // ?????????????????????????????????
        Image cut = ImgUtil.cut(image, new Rectangle(0, 0, min, min));
        Image scale = ImgUtil.scale(cut, 132, 132);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Img.from(scale).setTargetImageType(ImgUtil.IMAGE_TYPE_PNG).write(bos);
        // ??????????????????
        bytes = bos.toByteArray();
        String hash = Wss.generateHash(bytes);
        String url = storage.upload(FileType.IMAGE, bytes, hash);
        String mapping = resourceService.getMapping(FileType.IMAGE, hash);
        // ????????????
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getGuid, ShiroUser.getUserId());
        wrapper.set(SysUser::getHeadimgurl, mapping);
        Assert.isTrue(super.update(wrapper), "????????????", IllegalStateException::new);
        // ??????????????????
        resourceService.save(new ChatRecordFile(null, FileType.IMAGE.getKey(), url, hash, (long) bytes.length));
        // ??????????????????
        publisher.pushUserEvent(mapping, UserEnum.HEADIMG);
        return mapping;
    }

    @Override
    public void updateEmail(EmailCondition condition) {
        // ???????????????
        SysUser user = ShiroUser.get();
        String olds = user.getEmail();
        if (StrUtil.isNotEmpty(olds)) {
            String selfcode = redisClient.get(RedisTree.EMAIL.concat(olds));
            // ???????????????
            Assert.equals(selfcode, condition.getOcode(), "???????????????????????????", IllegalStateException::new);
        }
        // ???????????????
        String news = condition.getEmail();
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getEmail, news);
        Assert.isNull(this.getFirst(wrapper), "?????????????????????????????????", IllegalStateException::new);
        String newcode = redisClient.get(RedisTree.EMAIL.concat(news));
        Assert.equals(newcode, condition.getNcode(), "???????????????????????????", IllegalStateException::new);
        // ????????????
        wrapper.clear();
        wrapper.eq(SysUser::getGuid, user.getGuid());
        wrapper.set(SysUser::getEmail, news);
        Assert.isTrue(super.update(wrapper), "????????????", IllegalStateException::new);
        ShiroUser.set(SysUser::getEmail, news);
    }

    @Override
    public SysUser getUserInfo(String guid) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getGuid, guid);
        SysUser user = this.getFirst(wrapper);
        Assert.notNull(user, "????????????????????????", AccountException::new);
        return user;
    }

    @Override
    public UserRole switchRole(String target) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getGuid, target);
        SysUser first = getFirst(wrapper);
        UserRole role = first.getRole() == UserRole.ADMIN ? UserRole.USER : UserRole.ADMIN;
        wrapper.set(SysUser::getRole, role);
        update(wrapper);
        return role;
    }

    @Override
    public void updateAlias(String target, String alias) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getGuid, target);
        wrapper.set(SysUser::getAlias, alias);
        update(wrapper);
    }
}
