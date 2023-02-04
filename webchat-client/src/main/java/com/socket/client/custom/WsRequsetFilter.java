package com.socket.client.custom;

import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.util.Wss;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * Ws过滤器 收集请求信息
 */
@WebFilter
@Component
public class WsRequsetFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        Optional.ofNullable(req.getSession(false)).ifPresent(session -> {
            String platform = Wss.getPlatform(req.getHeader(Header.USER_AGENT.getValue()));
            session.setAttribute(Constants.PLATFORM, platform);
            session.setAttribute(Constants.IP, ServletUtil.getClientIP(req));
        });
        chain.doFilter(request, response);
    }
}
