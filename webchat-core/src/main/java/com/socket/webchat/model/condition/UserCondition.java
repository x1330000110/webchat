package com.socket.webchat.model.condition;

import lombok.Data;

/**
 * @date 2022/5/19
 */
@Data
public class UserCondition {
    /**
     * 用户UID
     */
    private String guid;
    /**
     * 要变更的数据
     */
    private String content;
}
