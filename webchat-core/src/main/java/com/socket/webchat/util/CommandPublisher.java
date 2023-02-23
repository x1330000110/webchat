package com.socket.webchat.util;

import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.custom.event.UserChangeEvent;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.model.command.impl.UserEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 命令推送封装
 */
@Component
public class CommandPublisher {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void pushGroupEvent(SysGroup group, GroupEnum command) {
        publisher.publishEvent(new GroupChangeEvent(publisher, group, command));
    }

    public void pushGroupEvent(SysGroupUser user, GroupEnum command) {
        publisher.publishEvent(new GroupChangeEvent(publisher, user, command));
    }

    public void pushPermissionEvent(String target, Object data, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, target, data, command));
    }

    public void pushPermissionEvent(String data, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, data, command));
    }

    public void pushPermissionEvent(String self, String target, String data, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, self, target, data, command));
    }

    public void pushUserEvent(String data, UserEnum command) {
        publisher.publishEvent(new UserChangeEvent(publisher, data, command));
    }

    public void pushUserEvent(String target, String data, UserEnum command) {
        publisher.publishEvent(new UserChangeEvent(publisher, target, data, command));
    }
}
