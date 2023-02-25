package com.socket.core.model.po;

import com.socket.core.model.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 屏蔽用户表
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShieldUser extends BaseModel {
    /**
     * 用户id
     */
    private String guid;
    /**
     * 屏蔽用户id
     */
    private String target;
}
