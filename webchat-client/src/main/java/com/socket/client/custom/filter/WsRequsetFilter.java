package com.socket.client.custom.filter;

import com.socket.webchat.constant.Constants;
import com.socket.webchat.util.Requests;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Ws过滤器 收集请求信息
 */
@WebFilter
@Component
public class WsRequsetFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest _request = (HttpServletRequest) request;
        HttpSession session = _request.getSession(false);
        if (session != null) {
            session.setAttribute(Constants.PLATFORM, Requests.getPlatform(_request));
            session.setAttribute(Constants.IP, Requests.getRemoteIP(_request));
        }
        chain.doFilter(request, response);
    }
}