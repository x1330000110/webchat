package com.socket.webchat.model.condition;

import lombok.Data;

/**
 * @date 2021/11/4
 */
@Data
public class PasswordCondition {
    /**
     * 要修改密码的邮箱（若为空 修改当前登录的账号）
     */
    private String email;
    /**
     * 邮箱验证码
     */
    private String code;
    /**
     * 新密码
     */
    private String password;
}
