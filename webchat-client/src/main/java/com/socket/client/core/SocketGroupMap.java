package com.socket.client.core;

import com.socket.core.model.command.Command;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.ws.WsMsg;
import com.socket.core.model.ws.WsUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ws群组管理器
 */
@Slf4j
@Component
public class SocketGroupMap extends ConcurrentHashMap<SysGroup, List<WsUser>> {
    /**
     * 向群组发送消息<br>
     *
     * @param wsmsg 消息
     */
    public void sendGroup(WsMsg wsmsg) {
        getGroupUsers(wsmsg.getTarget()).forEach(target -> target.send(wsmsg));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param gid 群组id
     * @return 成员
     */
    public List<WsUser> getGroupUsers(String gid) {
        return this.get(get(gid));
    }

    /**
     * 获取群组对象
     */
    public SysGroup get(String gid) {
        return this.keySet().stream()
                .filter(e -> e.getGuid().equals(gid))
                .findFirst()
                .orElse(null);
    }

    /**
     * @see #sendGroup(String, String, Command, Object)
     */
    public void sendGroup(String gid, String content, Command<?> command) {
        sendGroup(gid, content, command, null);
    }

    /**
     * 向群组发送系统消息<br>
     *
     * @param gid     群id
     * @param content 消息内容
     * @param command 消息类型
     * @param data    额外数据
     */
    public void sendGroup(String gid, String content, Command<?> command, Object data) {
        getGroupUsers(gid).forEach(wsuser -> wsuser.send(content, command, data));
    }
}
