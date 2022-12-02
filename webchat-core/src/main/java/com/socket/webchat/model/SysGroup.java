package com.socket.webchat.model;

import com.baomidou.mybatisplus.annotation.TableField;
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
    /**
     * 入群密码
     */
    private String password;
    /**
     * 是否需要密码
     */
    @TableField(exist = false)
    private Boolean needPass;
}
