package com.socket.core.service;

import com.socket.core.model.enums.LogType;
import com.socket.core.model.po.SysUserLog;

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
