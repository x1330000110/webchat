package com.socket.core.model.enums;

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
    USER;

    @JsonValue
    @Override
    public String toString() {
        return getRole();
    }

    public String getRole() {
        return name().toLowerCase();
    }

    @Override
    public String getValue() {
        return getRole();
    }
}
