package com.socket.client.exception;

import com.socket.webchat.model.enums.MessageType;
import lombok.Getter;

@Getter
public class SocketException extends RuntimeException {
    private final String callback;
    private final MessageType messageType;

    public SocketException(String callback, MessageType messageType) {
        super(callback);
        this.callback = callback;
        this.messageType = messageType;
    }
}
