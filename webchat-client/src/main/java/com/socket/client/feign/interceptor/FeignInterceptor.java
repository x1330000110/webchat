package com.socket.client.feign.interceptor;

import com.socket.core.constant.Constants;
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
        template.header(Constants.AUTH_SERVER_HEADER, Constants.AUTH_SERVER_KEY);
    }
}
