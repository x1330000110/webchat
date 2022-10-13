package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 群组成员信息
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
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

    public SysGroupUser(String groupId, String uid) {
        this.groupId = groupId;
        this.uid = uid;
    }
}
