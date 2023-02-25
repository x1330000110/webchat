package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 创建群组
 */
@Component
public class Create extends GroupChangeHandler {

    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        groupMap.put(group, new ArrayList<>());
    }
}
