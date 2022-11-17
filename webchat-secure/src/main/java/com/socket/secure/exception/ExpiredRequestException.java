package com.socket.secure.exception;

import com.socket.secure.constant.RequsetTemplate;

/**
 * Expired request exception
 *
 * @date 2022/3/28
 */
public class ExpiredRequestException extends InvalidRequestException {
    public ExpiredRequestException(RequsetTemplate template, Object... objs) {
        super(template, objs);
    }
}
