package com.socket.webchat.custom.listener;

import com.socket.webchat.model.SysUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class UserChangeEvent extends ApplicationEvent {
    @Getter
    private final SysUser user;

    public UserChangeEvent(Object source, SysUser user) {
        super(source);
        this.user = user;
    }
}
