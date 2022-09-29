package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 文件保存枚举
 *
 * @date 2022/6/8
 */
@Getter
public enum FilePath implements IEnum<String> {
    /**
     * 语音
     */
    AUDIO(0x150000),
    /**
     * 图片
     */
    IMAGE(0x300000),
    /**
     * 视频/文件
     */
    BLOB(0x1e00000);

    private final String directory;
    private final int size;

    FilePath(int size) {
        this.directory = '/' + getValue();
        this.size = size;
    }

    public static FilePath of(MessageType type) {
        switch (type) {
            case AUDIO:
                return AUDIO;
            case IMAGE:
                return IMAGE;
            case BLOB:
            case VIDEO:
                return BLOB;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getValue() {
        return name().toLowerCase();
    }
}
