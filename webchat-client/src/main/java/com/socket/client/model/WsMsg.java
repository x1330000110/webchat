package com.socket.client.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.socket.client.model.enums.CallbackTips;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.concurrent.ListenableFuture;

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

    WsMsg(String content, MessageType type, Object data) {
        this.sysmsg = true;
        this.content = content;
        this.type = type;
        this.data = data;
    }

    WsMsg(String uid, String target, String content, MessageType type, String mid) {
        this.uid = uid;
        this.target = target;
        this.content = content;
        this.type = type;
        this.mid = mid;
    }

    /**
     * 构造用户消息
     */
    public static WsMsg buildmsg(String uid, String target, ListenableFuture<String> content, MessageType type) {
        String mid = MD5.create().digestHex(uid + content + type.getName() + target + System.currentTimeMillis());
        return new WsMsg(uid, target, content, type, mid);
    }

    /**
     * 构造系统消息
     *
     * @param tips 内容
     * @param type 消息类型
     */
    public static WsMsg buildsys(CallbackTips tips, MessageType type) {
        return buildsys(tips, type, null);
    }

    /**
     * 构造系统消息
     *
     * @param tips 内容
     * @param type 消息类型
     * @param data 额外数据
     */
    public static WsMsg buildsys(CallbackTips tips, MessageType type, Object data) {
        return new WsMsg(tips.getReason(), type, data);
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
     */
    public void sendTo(WsUser target) {
        if (target.isOnline()) {
            target.getSocketSession().getAsyncRemote().sendText(target.encrypt(this));
        }
    }
}
