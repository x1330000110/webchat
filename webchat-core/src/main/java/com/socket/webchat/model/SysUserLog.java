package com.socket.webchat.model;

import com.socket.webchat.util.Wss;
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
     * 账号/用户ID
     */
    private String uid;
    /**
     * 最近登录ip地址
     */
    private String ip;
    /**
     * 登录平台
     */
    private String platform;

    public static SysUserLog buildLog() {
        SysUserLog log = new SysUserLog();
        log.uid = Wss.getUserId();
        log.ip = Wss.getRemoteIP();
        log.platform = Wss.getPlatform();
        return log;
    }
}
