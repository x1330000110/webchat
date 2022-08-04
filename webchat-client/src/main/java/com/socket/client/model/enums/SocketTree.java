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
    MUTE("mute:"),
    /**
     * 限制登录标记
     */
    LOCK("lock:"),
    /**
     * 发言标记
     */
    SPEAK("speak:");

    private final String prefix;

    SocketTree(String prefix) {
        this.prefix = prefix;
    }

    public String getPath(String suffix) {
        return prefix + suffix;
    }
}
