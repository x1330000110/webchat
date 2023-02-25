package com.socket.server.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.core.constant.Constants;
import com.socket.core.exception.AccountException;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.condition.LoginCondition;
import com.socket.core.model.condition.RegisterCondition;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.po.SysUser;
import com.socket.core.model.request.WxUser;
import com.socket.core.util.RedisClient;
import com.socket.secure.util.Assert;
import com.socket.server.request.WxAuth2Request;
import com.socket.server.service.SysGroupService;
import com.socket.server.service.SysUserService;
import com.socket.server.service.WxloginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

/**
 * 微信登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxloginServiceImpl implements WxloginService {
    private final SysGroupService sysGroupService;
    private final WxAuth2Request wxAuth2Request;
    private final SysUserService sysUserService;
    private final RedisClient<String> redis;

    @Override
    public SysUser authorize(String code, String uuid) {
        WxUser wxuser = wxAuth2Request.getUserInfo(code);
        Assert.notEmpty(wxuser.getOpenid(), "无效的openId", AccountException::new);
        String key = RedisTree.WXUUID.concat(uuid);
        // 二维码过期判断
        if (redis.exist(key)) {
            // 检查用户数据
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getOpenid, wxuser.getOpenid());
            SysUser user = sysUserService.getFirst(wrapper);
            String guid = Optional.ofNullable(user).map(BaseUser::getGuid).orElseGet(() -> {
                // 注册用户
                RegisterCondition condition = new RegisterCondition();
                condition.setName(StrUtil.sub(wxuser.getNickname(), 0, 6).trim());
                condition.setPass(Constants.DEFAULT_PASSWORD);
                condition.setImgurl(wxuser.getHeadimgurl());
                condition.setOpenid(wxuser.getOpenid());
                return sysUserService._register(condition).getGuid();
            });
            // 设置用户UID到Redis
            return redis.setIfPresent(key, guid, Constants.QR_CODE_EXPIRATION_TIME) ? user : null;
        }
        return null;
    }

    @Override
    public boolean login(String uuid) {
        String key = RedisTree.WXUUID.concat(uuid);
        String guid = redis.get(key);
        // key不存在（已过期）
        Assert.notNull(guid, "二维码已过期", AccountException::new);
        // 检查value是否被赋值[guid]
        if (StrUtil.isEmpty(guid)) {
            return false;
        }
        sysUserService.login(new LoginCondition(guid, Constants.DEFAULT_PASSWORD));
        return redis.remove(key);
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
        redis.set(RedisTree.WXUUID.concat(uuid), StrUtil.EMPTY, Constants.QR_CODE_EXPIRATION_TIME);
        return wxAuth2Request.getWxLoginURL(uuid);
    }
}
