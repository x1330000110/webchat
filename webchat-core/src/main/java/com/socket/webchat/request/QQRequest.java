package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.webchat.request.bean.QQAuth;
import com.socket.webchat.request.bean.QQAuthResp;
import com.socket.webchat.request.bean.QQUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * QQ请求工具
 */
@Slf4j
@Component
public class QQRequest {
    private static final String QQ_USER = "https://apibug.cn/api/qq/?apiKey=6db8a3a57cb4a5c723843946e1a0c651&qq={}";
    private static final String QQ_AUTH = "https://apibug.cn/api/qqlogin/?type=get&apiKey=fef9621137e76881ebb027bc18cff025";
    private static final String QQ_AUTH_VERIFY = "https://apibug.cn/api/qqlogin/?type=result&apiKey=fef9621137e76881ebb027bc18cff025&qrsig={}";

    /**
     * 获取qq昵称与头像
     *
     * @return 不存在返回null
     */
    public QQUser getInfo(String qq) {
        String body = HttpRequest.get(StrUtil.format(QQ_USER, qq)).execute().body();
        return JSONUtil.parseObj(body).toBean(QQUser.class);
    }

    /**
     * 获取QQ登录二维码
     *
     * @return {@link QQAuth}
     */
    public QQAuth getAuth() {
        String body = HttpRequest.get(QQ_AUTH).execute().body();
        JSONObject json = JSONUtil.parseObj(body);
        return json.getJSONObject("info").toBean(QQAuth.class);
    }

    /**
     * 验证QQ登录状态
     *
     * @param qrsig {@link QQAuth#getQrsig()}
     * @return {@link QQAuthResp}
     */
    public QQAuthResp verifyAuth(String qrsig) {
        String url = StrUtil.format(QQ_AUTH_VERIFY, qrsig);
        String body = HttpRequest.get(url).execute().body();
        log.info("Verify QQ: {}", body);
        JSONObject json = JSONUtil.parseObj(body);
        try {
            JSONObject info = json.getJSONObject("info");
            if (info.getStr("state") == null) {
                return new QQAuthResp("已失效");
            }
            // 解析cookie
            JSONObject object = new JSONObject();
            Arrays.stream(info.getStr("cookie").split(";"))
                    .map(e -> e.split("="))
                    .forEach(e -> object.set(e[0], e[1]));
            return object.toBean(QQAuthResp.class);
        } catch (Exception e) {
            return new QQAuthResp(json.getStr("info"));
        }
    }
}
