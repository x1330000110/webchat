package com.socket.webchat.model.condition;

import lombok.Data;

@Data
public class GroupCondition {
    /**
     * 群组ID
     */
    private String gid;
    /**
     * 成员ID
     */
    private String uid;
    /**
     * 群组名称
     */
    private String groupName;
}
