package com.socket.webchat.custom.listener;

import com.socket.webchat.model.ChatRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PermissionEvent extends ApplicationEvent {
    private final PermissionOperation operation;
    private ChatRecord record;
    private String target;
    private String data;

    public PermissionEvent(Object source, ChatRecord record, PermissionOperation operation) {
        super(source);
        this.record = record;
        this.operation = operation;
    }

    public PermissionEvent(Object source, String target, String data, PermissionOperation operation) {
        super(source);
        this.target = target;
        this.data = data;
        this.operation = operation;
    }

    public PermissionEvent(Object source, String data, PermissionOperation operation) {
        super(source);
        this.data = data;
        this.operation = operation;
    }
}
