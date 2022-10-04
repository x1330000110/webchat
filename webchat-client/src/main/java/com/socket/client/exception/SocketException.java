package com.socket.client.exception;

import com.socket.client.model.WsMsg;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.enums.MessageType;
import lombok.Getter;

@Getter
public class SocketException extends RuntimeException {
    private final Callback callback;
    private final MessageType messageType;

    public SocketException(Callback callback, MessageType messageType) {
        super(callback.getReason());
        this.callback = callback;
        this.messageType = messageType;
    }

    public WsMsg buildMessage() {
        return WsMsg.buildsys(callback, messageType);
    }
}
