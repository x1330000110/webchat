package com.socket.webchat.util;

import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.custom.event.UserChangeEvent;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.model.command.impl.UserEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件推送封装
 */
@Component
public class Publisher {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void pushGroupEvent(SysGroup group, GroupEnum command) {
        publisher.publishEvent(new GroupChangeEvent(publisher, group, command));
    }

    public void pushGroupEvent(SysGroupUser user, GroupEnum command) {
        publisher.publishEvent(new GroupChangeEvent(publisher, user, command));
    }

    public void pushPermissionEvent(String target, String data, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, target, data, command));
    }

    public void pushPermissionEvent(String data, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, data, command));
    }

    public void pushPermissionEvent(ChatRecord record, PermissionEnum command) {
        publisher.publishEvent(new PermissionEvent(publisher, record, command));
    }

    public void pushUserEvent(String data, UserEnum command) {
        publisher.publishEvent(new UserChangeEvent(publisher, command, data));
    }
}
