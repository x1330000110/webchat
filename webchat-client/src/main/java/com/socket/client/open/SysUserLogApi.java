package com.socket.client.open;

import com.socket.client.open.resp.FeignResp;
import com.socket.core.model.po.SysUserLog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@FeignClient("webchat-server")
@RequestMapping("/open/log")
public interface SysUserLogApi {
    /**
     * 创建日志
     */
    @PostMapping("/save")
    FeignResp<Void> saveLog(SysUserLog log);

    /**
     * 获取所有用户最新登录日志
     */
    @GetMapping("/getAll")
    FeignResp<Map<String, SysUserLog>> getLatestUserLogs();
}
