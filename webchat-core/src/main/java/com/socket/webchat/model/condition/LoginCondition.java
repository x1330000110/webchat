package com.socket.webchat.model.condition;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @date 2022/7/27
 */
@Data
@NoArgsConstructor
public class LoginCondition {
    /**
     * 自动登录
     */
    private boolean auto;
    /**
     * UID/邮箱
     */
    private String user;
    /**
     * 密码
     */
    private String pass;
    /**
     * 验证码
     */
    private String code;

    public LoginCondition(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }
}
