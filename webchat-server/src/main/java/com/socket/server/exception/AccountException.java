package com.socket.server.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * 账号验证失败异常
 */
public class AccountException extends AuthenticationException {
    public AccountException() {
    }

    public AccountException(String s) {
        super(s);
    }
}
