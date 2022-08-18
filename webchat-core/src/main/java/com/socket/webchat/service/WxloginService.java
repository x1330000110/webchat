package com.socket.webchat.service;

import com.socket.webchat.model.SysUser;
import com.socket.webchat.runtime.AccountException;

import javax.servlet.http.HttpServletResponse;

public interface WxloginService {
    /**
     * 微信认证入口
     *
     * @param code 微信登录凭证
     * @param uuid {@link java.util.UUID}
     * @return 认证成功返回登录的用户信息
     */
    SysUser authorize(String code, String uuid);

    /**
     * 通过UUID登录
     *
     * @param uuid {@link java.util.UUID}
     * @return 登录成功返回true
     * @throws AccountException 链接过期
     */
    boolean login(String uuid);

    /**
     * 生成微信登录二维码
     *
     * @param response {@link HttpServletResponse}
     * @param uuid     {@link java.util.UUID}
     */
    void generatePiccode(HttpServletResponse response, String uuid);

    /**
     * 微信登录链接
     *
     * @param uuid {@link java.util.UUID}
     */
    String getWxFastUrl(String uuid);
}
