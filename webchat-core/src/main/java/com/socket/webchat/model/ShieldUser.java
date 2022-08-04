package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 屏蔽用户表
 *
 * @since 2021/9/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShieldUser extends BaseModel {
    /**
     * 用户id
     */
    private String uid;
    /**
     * 屏蔽用户id
     */
    private String target;
}
