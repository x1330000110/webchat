package com.socket.server.controller;

import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.po.SysUserLog;
import com.socket.server.service.SysUserLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class SysLogController {
    private final SysUserLogService sysUserLogService;

    @GetMapping("/getLatest")
    public HttpStatus getLatestUserLogs() {
        Map<String, SysUserLog> map = sysUserLogService.getLatestUserLogs();
        return HttpStatus.SUCCESS.body(map);
    }

    @PostMapping("/save")
    public HttpStatus saveLog(@RequestBody SysUserLog log) {
        sysUserLogService.saveLog(log, null);
        return HttpStatus.SUCCESS.body();
    }
}
