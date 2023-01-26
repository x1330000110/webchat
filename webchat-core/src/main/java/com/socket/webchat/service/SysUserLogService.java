package com.socket.webchat.service;

import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.enums.LogType;

import java.util.Map;

public interface SysUserLogService extends BaseService<SysUserLog> {
    /**
     * 创建日志
     *
     * @param log 日志
     */
    void saveLog(SysUserLog log, LogType type);

    /**
     * 获取所有用户最新登录日志
     *
     * @return 日志列表
     */
    Map<String, SysUserLog> getLatestUserLogs();
}
