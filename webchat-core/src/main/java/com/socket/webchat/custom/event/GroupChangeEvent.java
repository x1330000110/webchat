package com.socket.webchat.custom.event;

import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.enums.GroupEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupChangeEvent extends ApplicationEvent {
    /**
     * 操作
     */
    private final GroupEnum operation;
    /**
     * 群组用户信息
     */
    private SysGroupUser user;
    /**
     * 群组信息
     */
    private SysGroup group;

    public GroupChangeEvent(Object source, SysGroupUser user, GroupEnum operation) {
        super(source);
        this.user = user;
        this.operation = operation;
    }

    public GroupChangeEvent(Object source, SysGroup group, GroupEnum operation) {
        super(source);
        this.group = group;
        this.operation = operation;
    }
}
