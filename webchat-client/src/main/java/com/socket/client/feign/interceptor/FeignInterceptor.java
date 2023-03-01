package com.socket.client.feign.interceptor;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.socket.client.util.ThreadUser;
import com.socket.core.constant.ChatConstants;
import com.socket.core.model.AuthUser;
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
        AuthUser user = ThreadUser.get();
        if (user != null) {
            String key = constants.getAuthServerKey();
            String header = constants.getAuthServerHeader();
            template.header(header, AES.encrypt(user.getUid(), key));
        }
    }
}
