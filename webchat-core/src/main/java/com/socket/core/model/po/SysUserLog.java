package com.socket.core.model.po;

import com.socket.core.model.base.BaseModel;
import com.socket.core.model.enums.LogType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户登录记录
 *
 * @date 2022/9/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserLog extends BaseModel {
    /**
     * 用户ID
     */
    private String guid;
    /**
     * 最近登录ip地址
     */
    private String ip;
    /**
     * IP所属省（市）
     */
    private String province;
    /**
     * 日志类型
     */
    private LogType type;
    /**
     * 登录平台
     */
    private String platform;
}
