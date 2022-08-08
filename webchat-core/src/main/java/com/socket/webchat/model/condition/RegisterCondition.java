package com.socket.webchat.model.condition;

import lombok.Data;

@Data
public class RegisterCondition {
    /**
     * 昵称
     */
    private String name;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 邮箱验证码
     */
    private String code;
    /**
     * 密码
     */
    private String pass;
}
