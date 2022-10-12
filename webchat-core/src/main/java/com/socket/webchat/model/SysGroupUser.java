package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群组成员信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysGroupUser extends BaseModel {
    /**
     * 群组id
     */
    private String groupId;
    /**
     * 群员id
     */
    private String uid;
}