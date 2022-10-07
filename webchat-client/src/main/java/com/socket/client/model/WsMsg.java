package com.socket.client.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.enums.MessageType;
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
    private MessageType type;
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
     * @param data     额外数据
     */
    public WsMsg(Callback callback, MessageType type, Object data) {
        this.sysmsg = true;
        this.content = callback.getReason();
        this.type = type;
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
    public WsMsg(String uid, String target, String content, MessageType type) {
        this.uid = uid;
        this.target = target;
        this.content = content;
        this.type = type;
        this.mid = generateMid();
    }

    /**
     * 生成消息MID
     */
    private String generateMid() {
        return MD5.create().digestHex(uid + content + type.getName() + target + System.currentTimeMillis());
    }

    /**
     * 转为未送达的消息
     */
    public WsMsg reject() {
        this.reject = true;
        return this;
    }

    /**
     * 转为已送达的消息
     */
    public WsMsg accept() {
        this.reject = false;
        return this;
    }

    /**
     * 消息发送的目标是否为群组
     */
    public boolean isGroup() {
        return target.startsWith(Constants.GROUP);
    }

    /**
     * HTML脚本过滤器
     */
    public void checkMessage() {
        if (content != null) {
            this.content = content.replaceAll("</?\\w+(\\s.+?)?>", "");
            this.content = StrUtil.sub(content, 0, Constants.MAX_MESSAGE_LENGTH);
        }
    }
}
