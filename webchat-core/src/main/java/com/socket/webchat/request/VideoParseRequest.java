package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.webchat.request.bean.VideoType;
import org.springframework.stereotype.Component;

/**
 * 视频解析端口
 */
@Component
public class VideoParseRequest {
    private static final String VIP_VIDEO_URL = "https://api.leafone.cn/api/jx?url={}";
    private static final String SHORT_VIDEO_URL = "https://api.leafone.cn/api/dsp?url={}";

    public String parseVideo(String url, VideoType type) {
        String body = HttpRequest.get(StrUtil.format(type.getUrl(), url)).execute().body();
        JSONObject json = JSONUtil.parseObj(body);
        if (json.getInt("code") == 200) {
            return json.getJSONObject("data").getStr("url");
        }
        return null;
    }
}
