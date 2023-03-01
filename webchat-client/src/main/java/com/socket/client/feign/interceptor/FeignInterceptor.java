package com.socket.client.feign.interceptor;

import com.socket.core.constant.ChatConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Feign自定义请求头
 */
@Configuration
@RequiredArgsConstructor
public class FeignInterceptor implements RequestInterceptor {
    private final ChatConstants constants;

    @Override
    public void apply(RequestTemplate template) {
        String key = constants.getAuthServerKey();
        String header = constants.getAuthServerHeader();
        template.header(header, key);
    }
}
