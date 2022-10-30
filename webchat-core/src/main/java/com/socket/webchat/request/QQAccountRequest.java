package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.socket.webchat.model.QQUser;
import org.springframework.stereotype.Component;

/**
 * 用于获取QQ头像/昵称（无需授权）
 */
@Component
public class QQAccountRequest {
    private static final String QQ_URL = "https://api.leafone.cn/api/qq?qq={}";

    /**
     * 获取qq昵称与头像
     *
     * @return 不存在返回null
     */
    public QQUser getInfo(String qq) {
        String body = HttpRequest.get(StrUtil.format(QQ_URL, qq)).execute().body();
        return JSONUtil.toBean(JSONUtil.parseObj(body).getJSONObject("data"), QQUser.class);
    }
}
