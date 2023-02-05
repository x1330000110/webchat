package com.socket.webchat.model.enums;

import com.socket.webchat.util.Enums;
import lombok.Getter;

/**
 * 文件规范枚举
 *
 * @date 2022/6/8
 */
@Getter
public enum FileType {
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

    private final String key;
    private final int code;
    private final int size;

    FileType(int size, int code) {
        this.key = Enums.key(this);
        this.size = size;
        this.code = code;
    }
}
