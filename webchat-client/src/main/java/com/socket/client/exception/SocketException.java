package com.socket.client.exception;

import com.socket.webchat.model.command.impl.CommandEnum;
import lombok.Getter;

@Getter
public class SocketException extends RuntimeException {
    private final String callback;
    private final CommandEnum messageType;

    public SocketException(String callback, CommandEnum messageType) {
        super(callback);
        this.callback = callback;
        this.messageType = messageType;
    }
}
