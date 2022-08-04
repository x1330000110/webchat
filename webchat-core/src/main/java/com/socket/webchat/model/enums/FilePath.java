package com.socket.webchat.model.enums;

import lombok.Getter;

/**
 * 文件保存枚举
 *
 * @date 2022/6/8
 */
@Getter
public enum FilePath {
    AUDIO(0x150000),
    IMAGE(0x300000),
    BLOB(0x1e00000);

    private final String name;
    private final int size;

    FilePath(int size) {
        this.name = '/' + name().toLowerCase();
        this.size = size;
    }

    public static FilePath of(MessageType type) {
        switch (type) {
            case AUDIO:
                return AUDIO;
            case IMG:
                return IMAGE;
            case BLOB:
            case VIDEO:
                return BLOB;
            default:
                throw new IllegalArgumentException();
        }
    }
}
