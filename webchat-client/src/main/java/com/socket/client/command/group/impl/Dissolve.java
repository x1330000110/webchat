package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupHandler;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

/**
 * 解散群组
 */
@Component
public class Dissolve extends GroupHandler {
    @Override
    public void execute(SysGroupUser user, SysGroup group) {
        SysGroup find = groupMap.getGroup(group.getGroupId());
        String tips = Callback.GROUP_DISSOLVE.format(find.getName());
        groupMap.sendGroup(group.getGroupId(), tips, GroupEnum.DISSOLVE);
        groupMap.remove(find);
    }
}
