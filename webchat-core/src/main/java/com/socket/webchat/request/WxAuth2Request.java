package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.properties.WxProperties;
import com.socket.webchat.request.bean.WxUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

/**
 * 微信授权登录管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxAuth2Request implements InitializingBean {
    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_userinfo&state={}#wechat_redirect";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo?access_token={}&openid={}&lang=zh_CN";
    private final RequestMappingHandlerMapping mapping;
    private final WxProperties properties;
    private String redirect;

    /**
     * 获取微信登录URL地址，注意需要标记{@linkplain WeChatRedirect}才能使用此方法
     *
     * @param uuid 登录凭证（由前端随机生成）
     * @return URL地址
     */
    public String getWxLoginURL(String uuid) {
        Assert.notEmpty(redirect, "微信授权登录出现异常", IllegalStateException::new);
        return StrUtil.format(AUTHORIZE, properties.getAppid(), redirect, uuid);
    }

    /**
     * 获取微信用户信息（昵称、头像等）
     *
     * @param code 授权码
     * @return 微信用户信息
     */
    public WxUser getUserInfo(String code) {
        String url = StrUtil.format(ACCESS_TOKEN_URL, properties.getAppid(), properties.getAppsecret(), code);
        JSONObject token = JSONUtil.parseObj(HttpRequest.get(url).execute().body());
        url = StrUtil.format(USER_INFO_URL, token.getStr("access_token"), token.getStr("openid"));
        return JSONUtil.toBean(HttpRequest.get(url).execute().body(), WxUser.class);
    }

    @Override
    public void afterPropertiesSet() {
        for (HandlerMethod handler : mapping.getHandlerMethods().values()) {
            Method method = handler.getMethod();
            WeChatRedirect anno = AnnotationUtils.findAnnotation(method, WeChatRedirect.class);
            if (anno == null) {
                continue;
            }
            RequestMapping mapp = AnnotationUtils.findAnnotation(handler.getBeanType(), RequestMapping.class);
            // mapping地址获取
            Function<String[], String> getURI = t -> Optional.ofNullable(t)
                    .map(e -> e.length == 0 ? null : e[0])
                    .orElse(StrUtil.EMPTY);
            // 格式化地址
            String root = getURI.apply(mapp == null ? null : mapp.value());
            String path = getURI.apply(anno.value());
            if (StrUtil.isNotEmpty(root)) {
                root = StrUtil.addPrefixIfNot(root, "/");
            }
            path = StrUtil.addPrefixIfNot(path, "/");
            // 拼接url
            String proxy = StrUtil.addPrefixIfNot(properties.getProxy(), "/");
            this.redirect = properties.getRedirect() + proxy + root + path;
            log.info("构造微信认证跳转地址：{}", redirect);
            return;
        }
        throw new BeanCreationException("找不到微信认证跳转地址，请在控制器下标记@WeChatRedirect");
    }
}
