package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;

/**
 * IP地址查询
 */
public class IPRequest {
    String URL = "https://apibug.cn/api/ip/?apiKey=1d381bf0c6d5db52490d77f00b9403bf&ip={}";

    /**
     * 获取IP地址所在城市
     */
    public String getCity(String ip) {
        String body = HttpRequest.get(StrUtil.format(URL, ip)).execute().body();
        return JSONUtil.parseObj(body).getStr("data");
    }

    /**
     * 获取IP所在省
     */
    public String getProvince(String ip) {
        String city = getCity(ip);
        if (city == null) {
            return null;
        }
        int index = city.indexOf("省");
        if (index == -1) {
            index = city.indexOf("市");
        }
        if (index == -1) {
            throw new IllegalArgumentException("无法解析的地区：" + city);
        }
        return city.substring(0, index + 1);
    }
}
