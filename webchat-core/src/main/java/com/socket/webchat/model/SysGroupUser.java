package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 群组成员信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
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

    public static SysGroupUser of(String groupId, String uid) {
        return new SysGroupUser().setGroupId(groupId).setUid(uid);
    }
}
