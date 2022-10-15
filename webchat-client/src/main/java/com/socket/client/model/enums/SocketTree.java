package com.socket.client.model.enums;

/**
 * socket目录树
 *
 * @date 2021/10/27
 */
public enum SocketTree {
    /**
     * 禁言标记
     */
    MUTE("MUTE:"),
    /**
     * 限制登录标记
     */
    LOCK("LOCK:"),
    /**
     * 发言标记
     */
    SPEAK("SPEAK:"),
    /**
     * 屏蔽列表
     */
    SHIELD("SHIELD:");

    private final String prefix;

    SocketTree(String prefix) {
        this.prefix = prefix;
    }

    public String concat(String suffix) {
        return prefix + suffix;
    }
}
