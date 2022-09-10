package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色枚举
 */
public enum UserRole implements IEnum<String> {
    /**
     * 所有者
     */
    OWNER,
    /**
     * 管理员
     */
    ADMIN,
    /**
     * 用户
     */
    USER,
    /**
     * 游客
     */
    GUEST;

    public String getRole() {
        return name().toLowerCase();
    }

    @JsonValue
    @Override
    public String toString() {
        return getRole();
    }

    @Override
    public String getValue() {
        return getRole();
    }
}
