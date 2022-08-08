package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.webchat.constant.WxProperties;
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
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={}&secret={}";
    private static final String OPENID_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token={}&openid={}";
    private final WxProperties properties;

    public String getAuthorize(String uuid) {
        return StrUtil.format(AUTHORIZE, properties.getAppid(), properties.getRedirect(), uuid);
    }

    public WxUser getUserInfo(String code) {
        String url = StrUtil.format(USER_INFO_URL, getAccessToken(), getOpenId(code));
        return JSONUtil.toBean(HttpRequest.get(url).execute().body(), WxUser.class);
    }

    private String getAccessToken() {
        String url = StrUtil.format(ACCESS_TOKEN_URL, properties.getAppid(), properties.getAppsecret());
        JSONObject object = JSONUtil.parseObj(HttpRequest.get(url).execute().body());
        return object.getStr("access_token", null);
    }

    private String getOpenId(String code) {
        String url = StrUtil.format(OPENID_URL, properties.getAppid(), properties.getAppsecret(), code);
        JSONObject object = JSONUtil.parseObj(HttpRequest.get(url).execute().body());
        return object.getStr("openid", null);
    }
}
