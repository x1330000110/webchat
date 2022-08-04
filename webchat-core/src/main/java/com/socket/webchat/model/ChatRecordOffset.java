package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询偏移量表
 *
 * @date 2022/4/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatRecordOffset extends BaseModel {
    /**
     * 发信人uid
     */
    private String uid;
    /**
     * 收信人uid
     */
    private String target;
    /**
     * 偏移量
     */
    private Long offset;
}
