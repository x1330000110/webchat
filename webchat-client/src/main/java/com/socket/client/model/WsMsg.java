package com.socket.client.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.socket.client.model.enums.Callback;
import com.socket.client.model.enums.Remote;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.websocket.Session;
import java.util.Objects;

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
     */
    WsMsg(Callback callback, MessageType type, Object data) {
        this.sysmsg = true;
        this.content = callback.getReason();
        this.type = type;
        this.data = data;
    }

    /**
     * 用户消息
     */
    WsMsg(String uid, String target, String content, MessageType type, String mid) {
        this.uid = uid;
        this.target = target;
        this.content = content;
        this.type = type;
        this.mid = mid;
    }

    /**
     * 未送达消息
     */
    public WsMsg(boolean reject, String mid, String content) {
        this.reject = reject;
        this.mid = mid;
        this.content = content;
    }

    /**
     * 构造用户消息
     *
     * @param uid     发起者
     * @param target  目标
     * @param content 内容
     * @param type    消息类型
     */
    public static WsMsg build(String uid, String target, String content, MessageType type) {
        String mid = MD5.create().digestHex(uid + content + type.getName() + target + System.currentTimeMillis());
        return new WsMsg(uid, target, content, type, mid);
    }

    /**
     * 构造系统消息
     *
     * @param callback 内容
     * @param type     消息类型
     */
    public static WsMsg build(Callback callback, MessageType type) {
        return new WsMsg(callback, type, null);
    }

    /**
     * 构造系统消息
     *
     * @param callback 内容
     * @param type     消息类型
     * @param data     额外数据
     */
    public static WsMsg build(Callback callback, MessageType type, Object data) {
        return new WsMsg(callback, type, data);
    }


    /**
     * 转为未送达的消息
     */
    public WsMsg reject() {
        return new WsMsg(true, mid, content);
    }

    /**
     * 转为已送达的消息
     */
    public WsMsg accept() {
        return new WsMsg(false, mid, content);
    }

    /**
     * 消息发送的目标是否为群组
     */
    public boolean isGroup() {
        return Objects.equals(target, Constants.GROUP);
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

    /**
     * 将消息发送至目标用户（目标不在线调用此方法没有任何效果）
     *
     * @param target 目标用户
     * @param type   发送方式
     */
    @SneakyThrows
    public void send(WsUser target, Remote type) {
        // 目标不在线
        if (!target.isOnline()) {
            return;
        }
        Session session = target.getSession();
        // 会话已关闭
        if (!session.isOpen()) {
            return;
        }
        // 发送消息
        String encrypt = target.encrypt(this);
        switch (type) {
            case ASYNC:
                session.getAsyncRemote().sendText(encrypt);
                break;
            case SYNC:
                session.getBasicRemote().sendText(encrypt);
            default:
                // ignore
        }
    }
}
