package com.socket.webchat.model;

import com.socket.webchat.model.enums.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息映射表
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ChatRecord extends BaseModel implements Comparable<ChatRecord> {
    /**
     * 系统消息
     */
    private boolean sysmsg;
    /**
     * 是否未读
     */
    private boolean unread;
    /**
     * 是否拒收
     */
    private boolean reject;
    /**
     * 发信人uid
     */
    private String uid;
    /**
     * 消息唯一标识
     */
    @EqualsAndHashCode.Include
    private String mid;
    /**
     * 消息类型
     */
    private MessageType type;
    /**
     * 附加数据
     */
    private Object data;
    /**
     * 收信人uid
     */
    private String target;
    /**
     * 消息内容
     */
    private String content;

    @Override
    public int compareTo(ChatRecord record) {
        return (int) (record.getCreateTime().getTime() - this.getCreateTime().getTime());
    }
}
