package com.socket.webchat.service.impl;

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
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.FTPClient;
import com.socket.webchat.custom.RedisClient;
import com.socket.webchat.mapper.SysUserLogMapper;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.model.condition.PasswordCondition;
import com.socket.webchat.model.condition.RegisterCondition;
import com.socket.webchat.model.enums.FilePath;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.runtime.AccountException;
import com.socket.webchat.runtime.UploadException;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.*;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysUserLogMapper sysUserLogMapper;
    private final RedisClient redisClient;
    private final FTPClient client;
    private final Email sender;

    @Override
    public void login(LoginCondition condition) {
        String code = condition.getCode(), uid = condition.getUser();
        // 优先验证邮箱验证码
        if (StrUtil.isNotEmpty(code)) {
            String email = uid;
            if (!email.contains("@")) {
                LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
                wrapper.eq(SysUser::getUid, uid);
                email = Opt.ofNullable(get(wrapper)).map(SysUser::getEmail).get();
                Assert.notNull(email, UnknownAccountException::new);
            }
            String key = RedisTree.EMAIL.concat(email);
            Object redisCode = redisClient.get(key);
            Assert.equals(redisCode, code, "验证码不正确", AccountException::new);
            Requests.set(Constants.OFFSITE);
            redisClient.remove(key);
        }
        // shiro登录
        SecurityUtils.getSubject().login(new UsernamePasswordToken(uid, condition.getPass(), condition.isAuto()));
        // 更新登录信息
        Optional.ofNullable(uid).ifPresent(e -> sysUserLogMapper.insert(new SysUserLog()));
    }

    @Override
    public void register(RegisterCondition condition) {
        // 检查
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getName, StrUtil.trim(condition.getName()));
        Assert.isNull(this.get(wrapper), "昵称已被使用", IllegalStateException::new);
        // 验证邮箱
        String key = RedisTree.EMAIL.concat(condition.getEmail());
        Assert.equals(condition.getCode(), redisClient.get(key), "验证码不正确", IllegalStateException::new);
        redisClient.remove(key);
        // 注册
        SysUser init = SysUser.newUser();
        init.setName(condition.getName());
        init.setEmail(condition.getEmail());
        init.setHash(Bcrypt.digest(condition.getPass()));
        super.save(init);
        // 通过邮箱登录
        this.login(new LoginCondition(condition.getEmail(), condition.getPass()));
    }

    @Override
    public String sendEmail(String email) {
        // 检查邮箱与UID
        if (!Validator.isEmail(email)) {
            final String uid = email;
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getUid, uid);
            SysUser user = getOne(wrapper);
            Assert.notNull(user, "找不到相关账号信息", IllegalStateException::new);
            Assert.isFalse(user.isDeleted(), "该账号已被永久限制登录", IllegalStateException::new);
            email = user.getEmail();
            Assert.notEmpty(email, "该账号未绑定邮箱信息", IllegalStateException::new);
        }
        // 检查重复发送间隔
        String key = RedisTree.INTERIM_EMAIL.concat(email);
        Assert.isFalse(redisClient.exist(key), "验证码发送过于频繁", IllegalStateException::new);
        // 检查发送次数上限
        key = RedisTree.LIMIT_EMAIL.concat(email);
        long count = redisClient.incr(key, 1, Constants.EMAIL_LIMIT_SENDING_INTERVAL, TimeUnit.HOURS);
        Assert.isTrue(count <= 3, "该账号验证码每日发送次数已达上限", IllegalStateException::new);
        redisClient.set(key, -1, Constants.EMAIL_SENDING_INTERVAL);
        // 发送邮件
        String code = sender.send(email);
        // 保存到redis 10分钟
        key = RedisTree.EMAIL.concat(email);
        redisClient.set(key, code, Constants.EMAIL_CODE_VALID_TIME, TimeUnit.MINUTES);
        return DesensitizedUtil.email(email);
    }

    @Override
    public boolean updatePassword(PasswordCondition condition) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        String email = condition.getEmail();
        SysUser user = Wss.getUser();
        if (StrUtil.isEmpty(email)) {
            Assert.notNull(user, "请输入邮箱", IllegalStateException::new);
            email = user.getEmail();
        }
        String key = RedisTree.EMAIL.concat(email);
        String code = redisClient.get(key);
        Assert.equals(code, condition.getCode(), "邮箱验证码不正确", IllegalStateException::new);
        // 通过邮箱修改密码
        wrapper.eq(SysUser::getEmail, email);
        wrapper.set(SysUser::getHash, Bcrypt.digest(condition.getPassword()));
        redisClient.remove(key);
        return super.update(wrapper);
    }

    @Override
    public SysUser updateMaterial(SysUser sysUser) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        // 清空uid
        sysUser.setUid(null);
        // 修改头像使用 /updateAvatar
        sysUser.setHeadimgurl(null);
        // 修改邮箱使用 /updateEmail
        sysUser.setEmail(null);
        // 若生日不为空 优先使用基于生日的年龄
        if (sysUser.getBirth() != null) {
            LocalDateTime time = sysUser.getBirth().atStartOfDay();
            long between = LocalDateTimeUtil.between(time, LocalDateTime.now(), ChronoUnit.YEARS);
            sysUser.setAge(Math.toIntExact(between));
        }
        wrapper.eq(SysUser::getUid, Wss.getUserId());
        return super.update(sysUser, wrapper) ? sysUser : null;
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
        String path = client.upload(FilePath.IMAGE, bos.toByteArray()).getMapping();
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getUid, Wss.getUserId());
        wrapper.set(SysUser::getHeadimgurl, path);
        return super.update(wrapper) ? path : null;
    }

    @Override
    public String updateEmail(EmailCondition condition) {
        // 验证原邮箱
        SysUser user = Wss.getUser();
        String selfemail = user.getEmail();
        if (StrUtil.isNotEmpty(selfemail)) {
            String selfcode = redisClient.get(RedisTree.EMAIL.concat(selfemail));
            // 对比验证码
            Assert.equals(selfcode, condition.getSelfcode(), "原邮箱验证码不正确", IllegalStateException::new);
        }
        // 验证新邮箱
        String newemail = condition.getUser();
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getEmail, newemail);
        Assert.isNull(this.get(wrapper), "该邮箱已被其他账号绑定", IllegalStateException::new);
        String newcode = redisClient.get(RedisTree.EMAIL.concat(newemail));
        Assert.equals(newcode, condition.getNewcode(), "新邮箱验证码不正确", IllegalStateException::new);
        // 更新邮箱
        wrapper.clear();
        wrapper.eq(SysUser::getUid, user.getUid());
        wrapper.set(SysUser::getEmail, newemail);
        return super.update(wrapper) ? condition.getNewcode() : null;
    }

    @Override
    public SysUser getUserInfo(String uid) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUid, uid);
        wrapper.eq(SysUser::isDeleted, 0);
        SysUser user = this.get(wrapper);
        Assert.notNull(user, "找不到此用户信息", AccountException::new);
        return user;
    }
}
