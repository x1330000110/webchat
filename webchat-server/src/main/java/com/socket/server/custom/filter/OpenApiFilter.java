package com.socket.server.custom.filter;

import cn.hutool.http.HttpStatus;
import com.socket.core.constant.ChatConstants;
import com.socket.server.custom.filter.anno.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 服务接口专用过滤器
 */
@Slf4j
@WebFilter
@Component
@RequiredArgsConstructor
public class OpenApiFilter implements Filter {
    private final RequestMappingHandlerMapping mapping;
    private final ChatConstants constants;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpreq = WebUtils.toHttp(request);
        HandlerMethod method = getHandlerMethod(httpreq);
        // 检查服务调用接口
        if (method != null && method.hasMethodAnnotation(OpenAPI.class)) {
            String value = httpreq.getHeader(constants.getAuthServerHeader());
            if (!constants.getAuthServerKey().equals(value)) {
                WebUtils.toHttp(response).setStatus(HttpStatus.HTTP_NOT_FOUND);
                log.warn("请求认证失败：{}", httpreq.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            // ServletRequestPathUtils.parseAndCache(request);
            HandlerExecutionChain chain = mapping.getHandler(request);
            if (chain != null) {
                return (HandlerMethod) chain.getHandler();
            }
        } catch (Exception e) {
            // ignore
        } finally {
            // ServletRequestPathUtils.clearParsedRequestPath(request);
        }
        return null;
    }
}
