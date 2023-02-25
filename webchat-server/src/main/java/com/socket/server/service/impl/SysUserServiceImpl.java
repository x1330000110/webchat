package com.socket.server.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.core.constant.Constants;
import com.socket.core.custom.publisher.CommandPublisher;
import com.socket.core.custom.storage.ResourceStorage;
import com.socket.core.exception.AccountException;
import com.socket.core.exception.UploadException;
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
import com.socket.core.util.*;
import com.socket.secure.util.Assert;
import com.socket.server.service.ResourceService;
import com.socket.server.service.SysGroupService;
import com.socket.server.service.SysUserService;
import com.socket.server.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.stereotype.Service;

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
    private final CommandPublisher publisher;
    private final RedisClient<Object> redis;
    private final ResourceStorage storage;
    private final EmailUtil sender;

    @Override
    public void login(LoginCondition condition) {
        String code = condition.getCode(), guid = condition.getUser();
        // 优先验证邮箱验证码
        if (StrUtil.isNotEmpty(code)) {
            String email = guid;
            if (!email.contains("@")) {
                LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
                wrapper.eq(SysUser::getGuid, guid);
                email = Opt.ofNullable(getFirst(wrapper)).map(SysUser::getEmail).get();
                Assert.notNull(email, "找不到指定账号", UnknownAccountException::new);
            }
            String key = RedisTree.EMAIL.concat(email);
            Object redisCode = redis.get(key);
            Assert.equals(redisCode, code, "验证码不正确", AccountException::new);
            Requests.set(Constants.OFFSITE);
            redis.remove(key);
        }
        // shiro登录
        SecurityUtils.getSubject().login(new UsernamePasswordToken(guid, condition.getPass(), condition.isAuto()));
    }

    @Override
    public void register(RegisterCondition condition) {
        // 验证邮箱
        String key = RedisTree.EMAIL.concat(condition.getEmail());
        Assert.equals(condition.getCode(), redis.get(key), "验证码不正确", IllegalStateException::new);
        redis.remove(key);
        // 注册
        SysUser user = _register(condition);
        // 登录
        this.login(new LoginCondition(user.getGuid(), condition.getPass()));
    }

    public SysUser _register(RegisterCondition condition) {
        SysUser user = SysUser.buildNewUser();
        user.setName(StrUtil.emptyToDefault(condition.getName(), "用户" + user.getGuid()));
        user.setHeadimgurl(condition.getImgurl());
        user.setEmail(condition.getEmail());
        user.setHash(Bcrypt.digest(condition.getPass()));
        user.setUin(condition.getUin());
        super.save(user);
        // 加入默认群组
        sysGroupService.joinGroup(Constants.DEFAULT_GROUP, user.getGuid());
        return user;
    }

    @Override
    public String sendEmail(String email) {
        // 检查邮箱与UID
        if (!Validator.isEmail(email)) {
            final String guid = email;
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getGuid, guid);
            SysUser user = getFirst(wrapper);
            Assert.notNull(user, "找不到相关账号信息", IllegalStateException::new);
            Assert.isFalse(user.isDeleted(), "该账号已被永久限制登录", IllegalStateException::new);
            email = user.getEmail();
            Assert.notEmpty(email, "该账号未绑定邮箱信息", IllegalStateException::new);
        }
        // 检查重复发送间隔
        String etk = RedisTree.EMAIL_TEMP.concat(email);
        Assert.isFalse(redis.exist(etk), "验证码发送过于频繁", IllegalStateException::new);
        redis.set(etk, -1, Constants.EMAIL_SENDING_INTERVAL);
        // 检查发送次数上限
        String elk = RedisTree.EMAIL_LIMIT.concat(email);
        long count = redis.incr(elk, 1, Constants.EMAIL_LIMIT_SENDING_INTERVAL, TimeUnit.HOURS);
        Assert.isTrue(count <= 3, "该账号验证码每日发送次数已达上限", IllegalStateException::new);
        // 发送邮件
        String code = sender.send(email);
        // 保存到redis 10分钟
        etk = RedisTree.EMAIL.concat(email);
        redis.set(etk, code, Constants.EMAIL_CODE_VALID_TIME, TimeUnit.MINUTES);
        return DesensitizedUtil.email(email);
    }

    @Override
    public boolean updatePassword(PasswordCondition condition) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        String email = condition.getEmail();
        SysUser user = ShiroUser.get();
        if (StrUtil.isEmpty(email)) {
            Assert.notNull(user, "请输入邮箱", IllegalStateException::new);
            email = user.getEmail();
        }
        String key = RedisTree.EMAIL.concat(email);
        String code = redis.get(key);
        Assert.equals(code, condition.getCode(), "邮箱验证码不正确", IllegalStateException::new);
        // 通过邮箱修改密码
        wrapper.eq(SysUser::getEmail, email);
        wrapper.set(SysUser::getHash, Bcrypt.digest(condition.getPassword()));
        redis.remove(key);
        return super.update(wrapper);
    }

    @Override
    public void updateMaterial(SysUser user) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        // 清空无法修改的字段
        user.setGuid(null);
        user.setHeadimgurl(null);
        user.setEmail(null);
        // 若生日不为空 优先使用基于生日的年龄
        if (user.getBirth() != null) {
            LocalDateTime time = user.getBirth().atStartOfDay();
            long between = LocalDateTimeUtil.between(time, LocalDateTime.now(), ChronoUnit.YEARS);
            user.setAge(Math.toIntExact(between));
        }
        wrapper.eq(SysUser::getGuid, ShiroUser.getUserId());
        Assert.isTrue(super.update(user, wrapper), "修改失败", IllegalStateException::new);
        // 推送变动事件
        publisher.pushUserEvent(user.getName(), UserEnum.NAME);
    }

    @Override
    public String updateAvatar(byte[] bytes) {
        Assert.isTrue(bytes.length <= 0x4b000, "图片大小超过限制", UploadException::new);
        // byte[]转图片
        BufferedImage image;
        try {
            image = ImgUtil.toImage(bytes);
        } catch (IllegalArgumentException | IORuntimeException e) {
            throw new UploadException("未能识别的图片格式");
        }
        // 创建图片文件
        int min = Math.min(image.getWidth(), image.getHeight());
        // 按最小宽度裁剪为正方形
        Image cut = ImgUtil.cut(image, new Rectangle(0, 0, min, min));
        Image scale = ImgUtil.scale(cut, 132, 132);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Img.from(scale).setTargetImageType(ImgUtil.IMAGE_TYPE_PNG).write(bos);
        // 图片映射地址
        bytes = bos.toByteArray();
        String hash = Wss.generateHash(bytes);
        String url = storage.upload(FileType.IMAGE, bytes, hash);
        String mapping = resourceService.getMapping(FileType.IMAGE, hash);
        // 保存头像
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getGuid, ShiroUser.getUserId());
        wrapper.set(SysUser::getHeadimgurl, mapping);
        Assert.isTrue(super.update(wrapper), "修改失败", IllegalStateException::new);
        // 保存文件映射
        resourceService.save(new ChatRecordFile(null, FileType.IMAGE.getKey(), url, hash, (long) bytes.length));
        // 推送变动事件
        publisher.pushUserEvent(mapping, UserEnum.HEADIMG);
        return mapping;
    }

    @Override
    public void updateEmail(EmailCondition condition) {
        // 验证原邮箱
        SysUser user = ShiroUser.get();
        String olds = user.getEmail();
        if (StrUtil.isNotEmpty(olds)) {
            String selfcode = redis.get(RedisTree.EMAIL.concat(olds));
            // 对比验证码
            Assert.equals(selfcode, condition.getOcode(), "原邮箱验证码不正确", IllegalStateException::new);
        }
        // 验证新邮箱
        String news = condition.getEmail();
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getEmail, news);
        Assert.isNull(this.getFirst(wrapper), "该邮箱已被其他账号绑定", IllegalStateException::new);
        String newcode = redis.get(RedisTree.EMAIL.concat(news));
        Assert.equals(newcode, condition.getNcode(), "新邮箱验证码不正确", IllegalStateException::new);
        // 更新邮箱
        wrapper.clear();
        wrapper.eq(SysUser::getGuid, user.getGuid());
        wrapper.set(SysUser::getEmail, news);
        Assert.isTrue(super.update(wrapper), "修改失败", IllegalStateException::new);
        ShiroUser.set(SysUser::getEmail, news);
    }

    @Override
    public SysUser getUserInfo(String guid) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getGuid, guid);
        SysUser user = this.getFirst(wrapper);
        Assert.notNull(user, "找不到此用户信息", AccountException::new);
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
