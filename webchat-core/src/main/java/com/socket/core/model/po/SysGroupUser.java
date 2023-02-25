package com.socket.core.model.po;

import com.socket.core.model.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 群组成员信息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysGroupUser extends BaseModel {
    /**
     * 群组id
     */
    private String gid;
    /**
     * 群员id
     */
    private String uid;

    public SysGroupUser(String gid, String uid) {
        this.gid = gid;
        this.uid = uid;
    }
}
