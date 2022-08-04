package com.socket.webchat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.runtime.AccountException;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.WxUser;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.request.WxAuth2Request;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.service.WxloginService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Bcrypt;
import com.socket.webchat.util.RedisValue;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 微信登录服务
 *
 * @since 2021/7/9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxloginServiceImpl implements WxloginService {
    private final StringRedisTemplate redisTemplate;
    private final WxAuth2Request wxAuth2Request;
    private final SysUserService sysUserService;

    @Override
    public SysUser authorize(String code, String uuid) {
        WxUser wxuser = wxAuth2Request.getUserInfo(code);
        Assert.notNull(wxuser.getOpenid(), "无效的openId", AccountException::new);
        RedisValue<String> redisUuid = RedisValue.of(redisTemplate, RedisTree.WX_UUID.getPath(uuid));
        // 二维码过期判断
        if (redisUuid.exist()) {
            // 转换UID格式
            String uid = Wss.toUID(wxuser.getOpenid());
            // 检查用户数据 不存在将被注册
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getUid, uid);
            SysUser user = sysUserService.getOne(wrapper);
            if (user == null) {
                user = SysUser.newInstance();
                BeanUtil.copyProperties(wxuser, user);
                user.setUid(uid);
                // 微信公众平台规范变动：2021-12-27起测试号等可能无法取到部分信息
                user.setName(StrUtil.emptyToDefault(wxuser.getNickname(), "用户" + uid));
                user.setHash(Bcrypt.digest(Constants.DEFAULT_PASSWORD));
                sysUserService.save(user);
            }
            // 设置用户UID到Redis
            return redisUuid.setIfPresent(uid, Constants.QR_CODE_EXPIRATION_TIME) ? user : null;
        }
        return null;
    }

    @Override
    public boolean login(String uuid) {
        RedisValue<String> redisUuid = RedisValue.of(redisTemplate, RedisTree.WX_UUID.getPath(uuid));
        // key不存在（已过期）
        Assert.isTrue(redisUuid.exist(), "二维码已过期", AccountException::new);
        // 检查value是否被赋值[uid]
        if (redisUuid.isEmpty()) {
            return false;
        }
        sysUserService.login(new LoginCondition(redisUuid.get(), Constants.DEFAULT_PASSWORD));
        return redisUuid.remove();
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
        RedisValue<String> redisUuid = RedisValue.of(redisTemplate, RedisTree.WX_UUID.getPath(uuid));
        redisUuid.set(StrUtil.EMPTY, Constants.QR_CODE_EXPIRATION_TIME);
        return wxAuth2Request.getAuthorize(uuid);
    }
}
