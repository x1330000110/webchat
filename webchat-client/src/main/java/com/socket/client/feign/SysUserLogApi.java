package com.socket.client.feign;

import com.socket.client.feign.response.FeignResponse;
import com.socket.core.model.po.SysUserLog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@FeignClient("webchat-server")
@RequestMapping("/log")
public interface SysUserLogApi {
    /**
     * 创建日志
     */
    @PostMapping("/save")
    FeignResponse<Void> saveLog(SysUserLog log);

    /**
     * 获取所有用户最新登录日志
     */
    @GetMapping("/getLatest")
    FeignResponse<Map<String, SysUserLog>> getLatestUserLogs();
}
