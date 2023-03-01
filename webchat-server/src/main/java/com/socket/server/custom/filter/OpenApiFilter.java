package com.socket.server.custom.filter;

import com.socket.core.constant.ChatConstants;
import com.socket.core.constant.Constants;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.AES;
import com.socket.secure.util.Assert;
import com.socket.server.custom.filter.anno.OpenApi;
import lombok.RequiredArgsConstructor;
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
@WebFilter
@Component
@RequiredArgsConstructor
public class OpenApiFilter implements Filter {
    private final RequestMappingHandlerMapping mapping;
    private final ChatConstants constants;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest _request = (HttpServletRequest) request;
        HandlerMethod method = getHandlerMethod(_request);
        // 检查服务调用接口
        if (method != null && method.hasMethodAnnotation(OpenApi.class)) {
            String header = constants.getAuthServerHeader();
            String encuid = _request.getHeader(header);
            try {
                Assert.notNull(encuid, InvalidRequestException::new);
                String key = constants.getAuthServerKey();
                String decuid = AES.decrypt(encuid, key);
                _request.setAttribute(Constants.CURRENT_USER_ID, decuid);
            } catch (InvalidRequestException e) {
                WebUtils.toHttp(response).setStatus(404);
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
