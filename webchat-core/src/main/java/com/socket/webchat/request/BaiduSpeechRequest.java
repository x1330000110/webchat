package com.socket.webchat.request;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/**
 * 百度语音识别相关功能
 */
@Component
public class BaiduSpeechRequest {
    private static final String OAUTH_2 = "https://openapi.baidu.com/oauth/2.0/token?grant_type=client_credentials&client_id=ObTPO56aMGDTP0Ttj9VjUk6a&client_secret=pIkvZf08xqi86ImHNgO14ir9nLpRLGqh";
    private static final String CONVERT_URL = "https://vop.baidu.com/server_api";

    /**
     * 音频文件转换文本
     *
     * @param bytes 文件数据
     * @return 文本
     */
    public String convertText(byte[] bytes) {
        JSONObject json = new JSONObject();
        json.set("cuid", "webchat");
        json.set("format", "wav");
        json.set("rate", "16000");
        json.set("channel", 1);
        json.set("token", getAccessToken());
        json.set("speech", Base64.encode(bytes));
        json.set("len", bytes.length);
        String body = HttpRequest.post(CONVERT_URL).body(json.toString()).execute().body();
        try {
            return JSONUtil.parseObj(body).getJSONArray("result").getStr(0);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 获得AccessToken
     */
    private String getAccessToken() {
        String body = HttpRequest.get(OAUTH_2).execute().body();
        return JSONUtil.parseObj(body).getStr("access_token");
    }
}
