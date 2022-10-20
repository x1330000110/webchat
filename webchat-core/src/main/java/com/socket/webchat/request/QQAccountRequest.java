package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import org.springframework.stereotype.Component;

/**
 * 用于获取QQ头像/昵称（无需授权）
 */
@Component
public class QQAccountRequest {
    private static final String NACKNAME = "https://v.api.aa1.cn/api/qqnicheng/index.php?qq={}";
    private static final String HEADIMG = "https://q2.qlogo.cn/headimg_dl?dst_uin={}&spec=100";

    /**
     * 获取qq昵称
     */
    public String getNackName(String qq) {
        String body = HttpRequest.get(StrUtil.format(NACKNAME, qq)).execute().body();
        return body.replaceFirst("QQ昵称：", "");
    }

    /**
     * 获取QQ头像
     */
    public byte[] getHeadimg(String qq) {
        return HttpRequest.get(StrUtil.format(HEADIMG, qq)).execute().bodyBytes();
    }
}
