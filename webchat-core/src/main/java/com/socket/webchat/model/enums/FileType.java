package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 文件规范枚举
 *
 * @date 2022/6/8
 */
@Getter
public enum FileType implements IEnum<String> {
    /**
     * 语音
     */
    AUDIO(3 * 1024 * 1024, 6353658),
    /**
     * 图片
     */
    IMAGE(1024 * 1024, 6353656),
    /**
     * 视频/文件
     */
    BLOB(100 * 1024 * 1024, 6353652);

    private final int code;
    private final int size;

    FileType(int size, int code) {
        this.size = size;
        this.code = code;
    }

    @Override
    public String getValue() {
        return name().toLowerCase();
    }
}
