package com.socket.secure.exception;

/**
 * Expired request exception
 *
 * @date 2022/3/28
 */
public class ExpiredRequestException extends InvalidRequestException {
    public ExpiredRequestException(String message) {
        super(message);
    }
}
