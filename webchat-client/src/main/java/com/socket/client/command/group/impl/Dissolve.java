package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
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
        String guid = group.getGuid();
        SysGroup find = groupMap.getGroup(guid);
        String tips = Callback.GROUP_DISSOLVE.format(find.getName());
        groupMap.sendGroup(guid, tips, GroupEnum.DISSOLVE, guid);
        groupMap.remove(find);
    }
}
