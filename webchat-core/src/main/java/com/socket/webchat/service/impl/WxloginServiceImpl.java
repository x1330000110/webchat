package com.socket.webchat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisClient;
import com.socket.webchat.custom.listener.UserChangeEvent;
import com.socket.webchat.exception.AccountException;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.WxUser;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.request.WxAuth2Request;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.service.WxloginService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Bcrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 微信登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxloginServiceImpl implements WxloginService {
    private final ApplicationEventPublisher publisher;
    private final WxAuth2Request wxAuth2Request;
    private final SysUserService sysUserService;
    private final RedisClient<String> redisClient;

    @Override
    public SysUser authorize(String code, String uuid) {
        WxUser wxuser = wxAuth2Request.getUserInfo(code);
        Assert.notNull(wxuser.getOpenid(), "无效的openId", AccountException::new);
        String key = RedisTree.WX_UUID.concat(uuid);
        // 二维码过期判断
        if (redisClient.exist(key)) {
            // 检查用户数据 不存在将被注册
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getOpenid, wxuser.getOpenid());
            SysUser user = sysUserService.getOne(wrapper);
            if (user == null) {
                user = SysUser.newUser();
                BeanUtil.copyProperties(wxuser, user);
                user.setName(StrUtil.sub(wxuser.getNickname(), 0, 6));
                user.setHash(Bcrypt.digest(Constants.WX_DEFAULT_PASSWORD));
                sysUserService.save(user);
                // 推送变动事件
                publisher.publishEvent(new UserChangeEvent(publisher, user));
            }
            // 设置用户UID到Redis
            return redisClient.setIfPresent(key, user.getUid(), Constants.QR_CODE_EXPIRATION_TIME) ? user : null;
        }
        return null;
    }

    @Override
    public boolean login(String uuid) {
        String key = RedisTree.WX_UUID.concat(uuid);
        String uid = redisClient.get(key);
        // key不存在（已过期）
        Assert.notNull(uid, "二维码已过期", AccountException::new);
        // 检查value是否被赋值[uid]
        if (StrUtil.isEmpty(uid)) {
            return false;
        }
        sysUserService.login(new LoginCondition(uid, Constants.WX_DEFAULT_PASSWORD));
        return redisClient.remove(key);
    }

    @Override
    public void generatePiccode(HttpServletResponse response, String uuid) {
        QrConfig config = QrConfig.create().setWidth(1000).setHeight(1000).setMargin(1);
        BufferedImage image = QrCodeUtil.generate(getWxFastUrl(uuid), config);
        try {
            ImgUtil.write(image, ImgUtil.IMAGE_TYPE_PNG, response.getOutputStream());
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public String getWxFastUrl(String uuid) {
        redisClient.set(RedisTree.WX_UUID.concat(uuid), StrUtil.EMPTY, Constants.QR_CODE_EXPIRATION_TIME);
        return wxAuth2Request.getWxLoginURL(uuid);
    }
}
