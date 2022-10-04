package com.socket.secure.exception;

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
}
