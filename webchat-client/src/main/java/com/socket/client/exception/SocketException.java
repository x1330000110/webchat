package com.socket.client.exception;

import com.socket.webchat.model.command.impl.MessageEnum;
import lombok.Getter;

@Getter
public class SocketException extends RuntimeException {
    private final String callback;
    private final MessageEnum messageType;

    public SocketException(String callback, MessageEnum messageType) {
        super(callback);
        this.callback = callback;
        this.messageType = messageType;
    }
}
