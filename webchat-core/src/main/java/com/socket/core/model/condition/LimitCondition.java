package com.socket.core.model.condition;

import lombok.Data;

@Data
public class LimitCondition {
    /**
     * 用户id
     */
    private String guid;
    /**
     * 限制时间（单位：秒）
     */
    private Long time;
}
