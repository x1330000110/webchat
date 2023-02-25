package com.socket.core.constant;

/**
 * 安全常量池
 */
public interface Constants {
    /**
     * shiro用户信息标记
     */
    String SUBJECT = "SUBJECT";
    /**
     * http session
     */
    String HTTP_SESSION = "HTTP_SESSION";
    /**
     * 用户IP地址
     */
    String IP = "REQUEST.IP";
    /**
     * 保存在session的平台标记
     */
    String PLATFORM = "REQUEST.PLATFORM";

    /**
     * 邮箱验证码有效时间（单位：分钟）
     */
    int EMAIL_CODE_VALID_TIME = 5;
    /**
     * 微信登录二维码过期时间（单位：秒）
     */
    long QR_CODE_EXPIRATION_TIME = 60;
    /**
     * 每次邮箱验证码发送间隔（单位: 秒）
     */
    int EMAIL_SENDING_INTERVAL = 60;
    /**
     * 邮箱验证码发送次数上限恢复时间间隔（单位: 小时）
     */
    int EMAIL_LIMIT_SENDING_INTERVAL = 12;

    /**
     * 默认密码，凭此密码可登录所有账号
     */
    String DEFAULT_PASSWORD = "IBOGSEVJVDKNPWOPIBUEQJKNOJWPFHIBGUOEJQNKCMOLWQOFPIHBUO";

    /**
     * 聊天记录历史记录同步数量
     */
    int SYNC_RECORDS_NUMS = 25;
    /**
     * 聊天系统10秒内允许发言的次数
     */
    int FREQUENT_SPEECH_THRESHOLD = 10;
    /**
     * 频繁发言禁言时间（单位：小时）
     */
    int FREQUENT_SPEECHES_MUTE_TIME = 6;
    /**
     * 允许的消息撤回时间（单位：秒）
     */
    int WITHDRAW_TIME = 120;

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
     * 最大消息长度
     */
    int MAX_MESSAGE_LENGTH = 300;
    /**
     * 缓存屏蔽列表的时间（单位：小时）
     */
    long SHIELD_CACHE_TIME = 24;
    /**
     * 最大群组创建数
     */
    int MAX_CREATE_GROUP_NUM = 3;
    /**
     * 最大可设置的入群密码长度
     */
    int MAX_GROUP_PASSWORD = 16;

    /**
     * 服务之间传输的请求头标识
     */
    String AUTH_SERVER_HEADER = "auth-server-key";
    /**
     * 服务之间传输的自定义密钥
     */
    String AUTH_SERVER_KEY = "RCTYVGUHBJINMKLKMOINJUGYTVFCRFVGYHBJNKM";
}
