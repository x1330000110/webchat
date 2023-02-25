package com.socket.core.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 日志类型
 */
public enum LogType implements IEnum<String> {
    LOGIN, LOGOUT;

    @Override
    public String getValue() {
        return name().toLowerCase();
    }
}
