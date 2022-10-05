package com.socket.secure.exception;

/**
 * Repeated request exception<br>
 * this exception can also occur with multiple requests from the same user within one second
 *
 * @date 2022/3/28
 */
public class RepeatedRequestException extends InvalidRequestException {
    public RepeatedRequestException(String message) {
        super(message);
    }
}
