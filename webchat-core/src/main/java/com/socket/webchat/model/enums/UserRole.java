package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

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

    public String getRole() {
        return name().toLowerCase();
    }

    public static UserRole of(String role) {
        return Arrays.stream(values()).filter(e -> e.getRole().equals(role)).findFirst().orElseThrow(IllegalStateException::new);
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
