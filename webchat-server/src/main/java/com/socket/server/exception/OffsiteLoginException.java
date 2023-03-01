package com.socket.server.exception;

/**
 * 异地登录检查
 *
 * @date 2022/1/31
 */
public class OffsiteLoginException extends AccountException {
    public OffsiteLoginException() {
    }

    public OffsiteLoginException(String s) {
        super(s);
    }
}
