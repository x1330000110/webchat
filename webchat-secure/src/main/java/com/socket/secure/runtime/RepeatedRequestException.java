package com.socket.secure.runtime;

import com.socket.secure.constant.SecureProperties;

/**
 * Repeated request exception<br>
 * If {@linkplain SecureProperties#isExactRequestTime()} is disable,
 * this exception can also occur with multiple requests from the same user within one second
 *
 * @date 2022/3/28
 */
public class RepeatedRequestException extends InvalidRequestException {
    public RepeatedRequestException(String message) {
        super(message);
    }
}
