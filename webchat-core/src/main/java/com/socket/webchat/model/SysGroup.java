package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群组信息
 */
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SysGroup extends BaseUser {
    /**
     * 群所有者uid
     */
    private String owner;
}
