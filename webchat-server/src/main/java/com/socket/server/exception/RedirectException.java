package com.socket.server.exception;

/**
 * 转发终止异常
 */
public class RedirectException extends RuntimeException {
    public RedirectException() {
    }

    public RedirectException(String s) {
        super(s);
    }
}