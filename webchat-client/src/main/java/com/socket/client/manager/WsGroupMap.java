package com.socket.client.manager;

import com.socket.client.exception.SocketException;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ws群组管理器
 */
@Slf4j
@Component
public class WsGroupMap extends ConcurrentHashMap<SysGroup, List<WsUser>> {
    /**
     * 向群组发送消息<br>
     *
     * @param wsmsg 消息
     */
    public void sendGroup(WsMsg wsmsg) {
        for (WsUser target : getGroupUsers(wsmsg.getTarget())) {
            target.send(wsmsg);
        }
    }

    /**
     * @see #sendGroup(String, String, Command, Object)
     */
    public void sendGroup(String groupId, String content, Command<?> type) {
        sendGroup(groupId, content, type, null);
    }

    /**
     * 向群组发送系统消息<br>
     *
     * @param groupId 群id
     * @param content 消息内容
     * @param type    消息类型
     * @param data    额外数据
     */
    public void sendGroup(String groupId, String content, Command<?> type, Object data) {
        for (WsUser wsuser : getGroupUsers(groupId)) {
            wsuser.send(content, type, data);
        }
    }

    /**
     * 获取群组对象
     */
    public SysGroup getGroup(String groupId) {
        return this.keySet().stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new SocketException(Callback.GROUP_NOT_FOUND.format(groupId), MessageType.DANGER));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param groupId 群组id
     * @return 成员
     */
    public List<WsUser> getGroupUsers(String groupId) {
        return this.get(getGroup(groupId));
    }
}
