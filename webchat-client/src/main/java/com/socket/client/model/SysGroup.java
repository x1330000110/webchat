package com.socket.client.model;

import com.socket.webchat.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群组信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysGroup extends BaseModel {
    /**
     * 群组id
     */
    @EqualsAndHashCode.Include
    private String groupId;
    /**
     * 群组名称
     */
    private String name;
    /**
     * 群所有者uid
     */
    private String owner;

    public WsUser toWsUser() {
        WsUser user = new WsUser();
        user.setName(name);
        user.setUid(groupId);
        return user;
    }
}
