package com.socket.core.constant;

/**
 * 常量池
 */
public interface Constants {
    /**
     * shiro用户信息标记
     */
    String SUBJECT = "SUBJECT";
    /**
     * 用户IP地址
     */
    String IP = "REQUEST.IP";
    /**
     * 保存在session的平台标记
     */
    String PLATFORM = "REQUEST.PLATFORM";
    /**
     * 默认密码，凭此密码可登录所有账号
     */
    String DEFAULT_PASSWORD = "IBOGSEVJVDKNPWOPIBUEQJKNOJWPFHIBGUOEJQNKCMOLWQOFPIHBUO";
    /**
     * 客户端群组前缀标识
     */
    String GROUP_PREFIX = "#";
    /**
     * 默认群组标记
     */
    String DEFAULT_GROUP = "Group";
    /**
     * 微信手机快捷登录标识
     */
    String WX_MOBILE = "$1";
    /**
     * 异地验证成功标记
     */
    String OFFSITE = "OFFSITE";
    /**
     * 系统管理员账号
     */
    String SYSTEM_UID = "10000";
    /**
     * 服务之间传输的请求标识
     */
    String AUTH_SERVER_KEY = "auth-server-key";
    /**
     * 认证密钥
     */
    String AUTH_TOKEN = "Token";
}
