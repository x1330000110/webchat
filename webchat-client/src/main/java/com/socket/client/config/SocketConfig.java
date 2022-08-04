package com.socket.client.config;

import cn.hutool.http.Header;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.util.Wss;
import org.apache.shiro.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;

@Configuration
public class SocketConfig extends ServerEndpointConfig.Configurator {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 手动初始化Spring任务Bean，无需标记@EnableScheduling注解（因为与WebSocket冲突）
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        Map<String, Object> map = config.getUserProperties();
        String userAgent = request.getHeaders().get(Header.USER_AGENT.getValue()).get(0);
        map.put(Constants.SUBJECT, SecurityUtils.getSubject());
        map.put(Constants.HTTP_SESSION, request.getHttpSession());
        map.put(Constants.PLATFORM, Wss.getPlatform(userAgent));
        super.modifyHandshake(config, request, response);
    }
}
