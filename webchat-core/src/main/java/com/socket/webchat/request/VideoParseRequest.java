package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

/**
 * 视频解析端口
 */
@Component
public class VideoParseRequest {
    private static final String VIP_URL = "https://api.leafone.cn/api/jx?url={}";
    private static final String SHORT_URL = "https://api.kit9.cn/api/aggregate_videos/api.php?link={}";

    /**
     * VIP视频解析
     */
    public static String parseVipVideo(String url) {
        return null;
    }

    /**
     * 短视频解析
     */
    public static String parseShortVideo(String url) {
        String body = HttpRequest.get(StrUtil.format(SHORT_URL, url)).execute().body();
        JSONObject json = JSONUtil.parseObj(body);
        if (json.getInt("code") == 200) {
            return json.getJSONObject("data").getStr("video_link");
        }
        return null;
    }
}
