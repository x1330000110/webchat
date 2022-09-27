package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.webchat.constant.properties.WxProperties;
import com.socket.webchat.model.WxUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 微信授权登录管理器
 */
@Component
@RequiredArgsConstructor
public class WxAuth2Request {
    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_userinfo&state={}#wechat_redirect";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo?access_token={}&openid={}&lang=zh_CN";
    private final WxProperties properties;

    public String getWxLoginURL(String uuid) {
        return StrUtil.format(AUTHORIZE, properties.getAppid(), properties.getRedirect(), uuid);
    }

    public WxUser getUserInfo(String code) {
        String url = StrUtil.format(ACCESS_TOKEN_URL, properties.getAppid(), properties.getAppsecret(), code);
        JSONObject token = JSONUtil.parseObj(HttpRequest.get(url).execute().body());
        url = StrUtil.format(USER_INFO_URL, token.getStr("access_token"), token.getStr("openid"));
        return JSONUtil.toBean(HttpRequest.get(url).execute().body(), WxUser.class);
    }

    private JSONObject getToken(String code) {
        String url = StrUtil.format(ACCESS_TOKEN_URL, properties.getAppid(), properties.getAppsecret(), code);
        return JSONUtil.parseObj(HttpRequest.get(url).execute().body());
    }
}
