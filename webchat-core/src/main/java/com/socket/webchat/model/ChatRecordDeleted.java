package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

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
     * 消息ID
     */
    private String mid;
    /**
     * 目标uid
     */
    private String target;
    /**
     * 消息创建时间
     */
    private Date recordTime;
}
