package com.socket.webchat.custom.event;

import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.util.Wss;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PermissionEvent extends ApplicationEvent {
    private final PermissionEnum operation;
    private final String self;
    private final String target;
    private final Object param;

    public PermissionEvent(Object source, Object param, PermissionEnum operation) {
        this(source, null, param, operation);
    }

    public PermissionEvent(Object source, String target, Object param, PermissionEnum operation) {
        this(source, Wss.getUserId(), target, param, operation);
    }

    public PermissionEvent(Object source, String self, String target, Object param, PermissionEnum operation) {
        super(source);
        this.self = self;
        this.target = target;
        this.param = param;
        this.operation = operation;
    }
}
