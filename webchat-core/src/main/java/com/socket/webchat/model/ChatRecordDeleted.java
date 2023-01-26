package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息移除标记表
 *
 * @date 2022/8/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatRecordDeleted extends BaseModel {
    /**
     * 发信人uid
     */
    private String guid;
    /**
     * 目标uid
     */
    private String target;
    /**
     * {@link ChatRecord#getId()}
     */
    private Long recordId;
}
