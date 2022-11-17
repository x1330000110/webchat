package com.socket.secure.exception;

import com.socket.secure.constant.RequsetTemplate;

/**
 * Repeated request exception<br>
 * this exception can also occur with multiple requests from same user within one second
 *
 * @date 2022/3/28
 */
public class RepeatedRequestException extends InvalidRequestException {
    public RepeatedRequestException(RequsetTemplate template, Object... objs) {
        super(template, objs);
    }
}
