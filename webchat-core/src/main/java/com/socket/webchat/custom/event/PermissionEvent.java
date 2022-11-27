package com.socket.webchat.custom.event;

import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.enums.PermissionEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PermissionEvent extends ApplicationEvent {
    private final PermissionEnum operation;
    private ChatRecord record;
    private String target;
    private String data;

    public PermissionEvent(Object source, ChatRecord record, PermissionEnum operation) {
        super(source);
        this.record = record;
        this.operation = operation;
    }

    public PermissionEvent(Object source, String target, String data, PermissionEnum operation) {
        super(source);
        this.target = target;
        this.data = data;
        this.operation = operation;
    }

    public PermissionEvent(Object source, String data, PermissionEnum operation) {
        super(source);
        this.data = data;
        this.operation = operation;
    }
}
