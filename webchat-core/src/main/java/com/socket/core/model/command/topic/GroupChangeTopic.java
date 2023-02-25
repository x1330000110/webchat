package com.socket.core.model.command.topic;

import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群组变更消息
 */
@Data
@NoArgsConstructor
public class GroupChangeTopic {
    /**
     * 操作
     */
    private GroupEnum operation;
    /**
     * 群组用户信息
     */
    private SysGroupUser user;
    /**
     * 群组信息
     */
    private SysGroup group;

    public GroupChangeTopic(SysGroupUser user, GroupEnum operation) {
        this.user = user;
        this.operation = operation;
    }

    public GroupChangeTopic(SysGroup group, GroupEnum operation) {
        this.group = group;
        this.operation = operation;
    }
}
