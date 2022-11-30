package com.socket.client.model;

import cn.hutool.crypto.digest.MD5;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.util.Wss;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息
 */
@Data
@NoArgsConstructor
public class WsMsg {
    /**
     * 是否为系统消息
     */
    private boolean sysmsg;
    /**
     * 消息是否被屏蔽
     */
    private boolean reject;
    /**
     * 消息唯一id（系统消息没有MID）
     */
    private String mid;
    /**
     * 消息类型
     */
    private String type;
    /**
     * 用户uid
     */
    private String uid;
    /**
     * 目标uid
     */
    private String target;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 附加数据
     */
    private Object data;

    /**
     * 系统消息
     *
     * @param callback 内容
     * @param type     消息类型
     */
    public WsMsg(String callback, Command<?> type) {
        this(callback, type, null);
    }

    /**
     * 系统消息
     *
     * @param callback 内容
     * @param type     消息类型
     * @param data     额外数据
     */
    public WsMsg(String callback, Command<?> type, Object data) {
        this.sysmsg = true;
        this.content = callback;
        this.type = type.getName();
        this.data = data;
    }

    /**
     * 用户消息
     *
     * @param uid     发起者
     * @param target  目标
     * @param content 内容
     * @param type    消息类型
     */
    public WsMsg(String uid, String target, String content, Command<?> type) {
        this.uid = uid;
        this.target = target;
        this.content = content;
        this.type = type.getName();
        this.mid = generateMid();
    }

    /**
     * 生成消息MID
     */
    private String generateMid() {
        return MD5.create().digestHex(uid + content + type + target + System.currentTimeMillis());
    }

    /**
     * 消息发送的目标是否为群组
     */
    public boolean isGroup() {
        return Wss.isGroup(target);
    }
}
