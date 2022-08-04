package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

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

    @Override
    public String toString() {
        return getRole();
    }

    @Override
    public String getValue() {
        return getRole();
    }
}
