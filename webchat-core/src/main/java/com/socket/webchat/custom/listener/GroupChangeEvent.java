package com.socket.webchat.custom.listener;

import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.enums.GroupOperation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupChangeEvent extends ApplicationEvent {
    /**
     * 操作
     */
    private final GroupOperation operation;
    /**
     * 群组信息
     */
    private SysGroupUser groupUser;
    /**
     * 用户信息
     */
    private SysGroup group;

    public GroupChangeEvent(Object source, SysGroupUser groupUser, GroupOperation operation) {
        super(source);
        this.groupUser = groupUser;
        this.operation = operation;
    }

    public GroupChangeEvent(Object source, SysGroup group, GroupOperation operation) {
        super(source);
        this.group = group;
        this.operation = operation;
    }
}
