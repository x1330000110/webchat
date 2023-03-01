package com.socket.client.feign.interceptor;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.socket.client.util.ThreadUser;
import com.socket.core.constant.Constants;
import com.socket.core.model.AuthUser;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;

/**
 * Feign自定义请求头
 */
@Configuration
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        AuthUser user = ThreadUser.get();
        // 使用用户密钥加密
        if (user != null) {
            template.header(Constants.AUTH_SERVER_KEY, AES.encrypt(user.getUid(), user.getKey()));
        }
    }
}
