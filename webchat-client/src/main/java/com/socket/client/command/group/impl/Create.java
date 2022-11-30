package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupHandler;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 创建群组
 */
@Component
public class Create extends GroupHandler {

    @Override
    public void execute(SysGroupUser user, SysGroup group) {
        groupMap.put(group, new ArrayList<>());
    }
}
