package com.socket.client.command.group.impl;

import cn.hutool.core.util.StrUtil;
import com.socket.client.command.group.GroupChangeHandler;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import org.springframework.stereotype.Component;

/**
 * 解散群组
 */
@Component
public class Dissolve extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        String guid = group.getGuid();
        SysGroup find = groupMap.get(guid);
        String tips = StrUtil.format("群 {} 已被创建者解散", find.getName());
        groupMap.sendGroup(guid, tips, GroupEnum.DISSOLVE, guid);
        groupMap.remove(find);
    }
}
