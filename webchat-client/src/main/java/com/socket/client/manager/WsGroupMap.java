package com.socket.client.manager;

import com.socket.client.exception.SocketException;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.MessageEnum;
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
        getGroupUsers(wsmsg.getTarget()).forEach(target -> target.send(wsmsg));
    }

    /**
     * @see #sendGroup(String, String, Command, Object)
     */
    public void sendGroup(String gid, String content, Command<?> type) {
        sendGroup(gid, content, type, null);
    }

    /**
     * 向群组发送系统消息<br>
     *
     * @param gid     群id
     * @param content 消息内容
     * @param type    消息类型
     * @param data    额外数据
     */
    public void sendGroup(String gid, String content, Command<?> type, Object data) {
        getGroupUsers(gid).forEach(wsuser -> wsuser.send(content, type, data));
    }

    /**
     * 获取群组对象
     */
    public SysGroup getGroup(String gid) {
        return this.keySet().stream()
                .filter(e -> e.getGuid().equals(gid))
                .findFirst()
                .orElseThrow(() -> new SocketException(Callback.GROUP_NOT_FOUND.format(gid), MessageEnum.DANGER));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param gid 群组id
     * @return 成员
     */
    public List<WsUser> getGroupUsers(String gid) {
        return this.get(getGroup(gid));
    }
}
