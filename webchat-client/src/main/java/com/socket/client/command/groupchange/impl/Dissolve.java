package com.socket.client.command.groupchange.impl;

import com.socket.client.command.groupchange.GroupChangeHandler;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

/**
 * 解散群组
 */
@Component
public class Dissolve extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        String groupId = group.getGroupId();
        SysGroup find = groupMap.getGroup(groupId);
        String tips = Callback.GROUP_DISSOLVE.format(find.getName());
        groupMap.sendGroup(groupId, tips, GroupEnum.DISSOLVE, groupId);
        groupMap.remove(find);
    }
}
