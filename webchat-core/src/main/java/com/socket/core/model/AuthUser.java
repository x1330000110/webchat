package com.socket.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    /**
     * 账号
     */
    private String uid;
    /**
     * 加密密钥
     */
    private String key;
}
