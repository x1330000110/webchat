package com.socket.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.core.mapper.SysUserLogMapper;
import com.socket.core.model.enums.LogType;
import com.socket.core.model.po.SysUserLog;
import com.socket.core.request.IPRequest;
import com.socket.core.service.SysUserLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserLogServiceImpl extends ServiceImpl<SysUserLogMapper, SysUserLog> implements SysUserLogService {
    private final SysUserLogMapper sysUserLogMapper;
    private final IPRequest ipRequest;

    @Override
    public void saveLog(SysUserLog log, LogType type) {
        String ip = log.getIp();
        log.setType(type);
        log.setProvince(ipRequest.getProvince(ip));
        log.setCreateTime(null);
        log.setUpdateTime(null);
        save(log);
    }

    @Override
    public Map<String, SysUserLog> getLatestUserLogs() {
        List<SysUserLog> logs = sysUserLogMapper.selectLatestLogonLogs();
        return logs.stream().collect(Collectors.toMap(SysUserLog::getGuid, Function.identity()));
    }
}
