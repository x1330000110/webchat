package com.socket.webchat.model.condition;

import lombok.Data;

/**
 * @date 2022/4/12
 */
@Data
public class MessageCondition {
    /**
     * 发起者uid
     */
    private String guid;
    /**
     * 目标uid
     */
    private String target;
    /**
     * 消息mid
     */
    private String mid;
}
