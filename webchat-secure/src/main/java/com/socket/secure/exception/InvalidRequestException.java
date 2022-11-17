package com.socket.secure.exception;

import com.socket.secure.constant.RequsetTemplate;

/**
 * Security Authentication Failed Exception <br>
 *
 * @date 2022/1/22
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException() {
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(RequsetTemplate template, Object... objs) {
        super(template.format(objs));
    }
}
