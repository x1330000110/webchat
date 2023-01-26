package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/**
 * IP地址查询
 */
@Component
public class IPRequest {
    private static final String URL = "http://ip-api.com/json/{}?lang=zh-CN";

    /**
     * 获取IP地址所在省
     */
    public String getProvince(String ip) {
        String body = HttpRequest.get(StrUtil.format(URL, ip)).execute().body();
        String regionName = JSONUtil.parseObj(body).getStr("regionName");
        return StrUtil.emptyToDefault(regionName, "未知");
    }
}
