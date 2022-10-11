package com.socket.webchat.model.enums;

import lombok.Getter;

/**
 * 所有者部分权限状态枚举
 */
public enum Setting {
    /**
     * 小冰AI会话接管
     */
    AI_MESSAGE,
    /**
     * 敏感关键词审查
     */
    SENSITIVE_WORDS,
    /**
     * 全员禁言
     */
    ALL_MUTE;

    @Getter
    private final String key;

    Setting() {
        this.key = name();
    }
}
