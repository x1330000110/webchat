package com.socket.webchat.request;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/**
 * 用于获取QQ头像/昵称（无需授权）
 */
@Component
public class QQAccountRequest {
    private static final String NACKNAME = "https://users.qzone.qq.com/fcg-bin/cgi_get_portrait.fcg?uins={}";
    private static final String HEADIMG = "https://q2.qlogo.cn/headimg_dl?dst_uin={}&spec=100";

    /**
     * 获取qq昵称
     */
    public String getNackName(String qq) {
        String body = HttpRequest.get(StrUtil.format(NACKNAME, qq)).execute().body();
        try {
            String s = ReUtil.findAllGroup1("\\w+\\((.+)\\)", body).get(0);
            JSONObject object = JSONUtil.parseObj(s);
            return JSONUtil.parseArray(object.values().iterator().next()).get(6).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取QQ头像
     */
    public byte[] getHeadimg(String qq) {
        return HttpRequest.get(StrUtil.format(HEADIMG, qq)).execute().bodyBytes();
    }
}
