package com.socket.webchat.custom.listener;

import org.springframework.context.ApplicationEvent;

public class PermissionEvent extends ApplicationEvent {
    public PermissionEvent(Object source) {
        super(source);
    }
}
