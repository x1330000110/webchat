package com.socket.webchat.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.properties.WxProperties;
import com.socket.webchat.request.bean.WxUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 微信授权登录管理器
 */
@Slf4j
@Component
public class WxAuth2Request implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent> {
    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_userinfo&state={}#wechat_redirect";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo?access_token={}&openid={}&lang=zh_CN";
    private final WxProperties properties;
    private final String packagePrefix;
    private String redirect;

    public WxAuth2Request(WxProperties properties) {
        this.properties = properties;
        this.packagePrefix = getPackagePrefix();
    }

    /**
     * 获取当前包前缀
     */
    private String getPackagePrefix() {
        String packageName = getClass().getPackage().getName();
        int idx = packageName.indexOf(".", packageName.indexOf(".") + 1);
        return packageName.substring(0, idx > -1 ? idx : packageName.length());
    }

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
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        if (StrUtil.isNotEmpty(redirect) || !clazz.getPackage().getName().startsWith(packagePrefix)) {
            return bean;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            WeChatRedirect annotation = method.getAnnotation(WeChatRedirect.class);
            if (annotation != null) {
                RequestMapping outer = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
                String uri = outer == null ? "" : Arrays.stream(outer.value()).findFirst().orElse("");
                String redirect = Arrays.stream(annotation.value()).findFirst().orElse("");
                uri = uri.startsWith("/") ? uri : "/" + uri;
                redirect = redirect.startsWith("/") ? redirect : "/" + redirect;
                this.redirect = String.join("", properties.getDomainService(), uri, redirect);
                break;
            }
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (StrUtil.isEmpty(redirect)) {
            log.warn("微信登录授权跳转地址为空，无法使用微信授权登录");
        }
    }
}
