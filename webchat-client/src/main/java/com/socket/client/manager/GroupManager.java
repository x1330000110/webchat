package com.socket.client.manager;

import com.socket.core.model.command.Command;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.socket.SocketMessage;
import com.socket.core.model.socket.SocketUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ws群组管理器
 */
@Slf4j
@Component
public class GroupManager extends ConcurrentHashMap<SysGroup, List<SocketUser>> {
    /**
     * 向群组发送消息<br>
     *
     * @param message 消息
     */
    public void sendGroup(SocketMessage message) {
        getGroupUsers(message.getTarget()).forEach(target -> target.send(message));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param gid 群组id
     * @return 成员
     */
    public List<SocketUser> getGroupUsers(String gid) {
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
        getGroupUsers(gid).forEach(user -> user.send(content, command, data));
    }
}
