package com.socket.webchat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.mapper.SysUserLogMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.enums.LogType;
import com.socket.webchat.request.IPRequest;
import com.socket.webchat.service.SysUserLogService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserLogServiceImpl extends ServiceImpl<SysUserLogMapper, SysUserLog> implements SysUserLogService {
    private final IPRequest ipRequest;

    @Override
    public void saveLog(SysUserLog log, LogType type) {
        String ip = log.getIp();
        // 保存数据
        log.setType(type);
        log.setIp(ip);
        log.setRemoteProvince(ipRequest.getProvince(ip));
        log.setCreateTime(null);
        log.setUpdateTime(null);
        save(log);
    }

    @Override
    public Map<String, SysUserLog> getUserLogs() {
        QueryWrapper<SysUserLog> wrapper = Wrappers.query();
        wrapper.select(Wss.columnToString(SysUserLog::getGuid),
                Wss.selectMax(BaseModel::getCreateTime),
                Wss.columnToString(SysUserLog::getRemoteProvince));
        wrapper.lambda().groupBy(SysUserLog::getGuid);
        List<SysUserLog> userLogs = list(wrapper);
        return userLogs.stream().collect(Collectors.toMap(SysUserLog::getGuid, Function.identity()));
    }
}
