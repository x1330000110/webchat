package com.socket.webchat.constant;

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
     * 邮箱验证码有效时间（单位：分钟）
     */
    int EMAIL_CODE_VALID_TIME = 5;
    /**
     * 微信登录二维码过期时间
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
     * 微信登录时设置的默认密码
     */
    String WX_DEFAULT_PASSWORD = "IBOGSEVJVDKNPWOPIBUEQJKNOJWPFHIBGUOEJQNKCMOLWQOFPIHBUO";
    /**
     * 聊天记录历史记录同步数量
     */
    int SYNC_RECORDS_NUMS = 25;
    /**
     * 聊天系统10秒内允许发言的次数
     */
    int FREQUENT_SPEECH_THRESHOLD = 10;
    /**
     * 频繁发言禁言时间（单位：秒）
     */
    int FREQUENT_SPEECHES_MUTE_TIME = 1800;
    /**
     * 允许的消息撤回时间（单位：秒）
     */
    int WITHDRAW_MESSAGE_TIME = 120;
    /**
     * 文件过期时间（单位：天）
     */
    int FILE_EXPIRED_DAYS = 3;
    /**
     * 客户端群组标识
     */
    String GROUP = "Group";
    /**
     * 微信手机快捷登录标识
     */
    String WX_MOBILE = "$1";
    /**
     * 消息队列kafka topic标记
     */
    String KAFKA_RECORD = "RECORD";
    /**
     * 异地验证成功标记
     */
    String OFFSITE = "OFFSITE";
    /**
     * 平台标记
     */
    String PLATFORM = "PLATFORM";
    /**
     * 永久限制登录标记
     */
    String FOREVER = "FOREVER";
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
}
