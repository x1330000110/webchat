package com.socket.secure.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentParser;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.event.entity.InitiatorEvent;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.filter.validator.RepeatValidator;
import com.socket.secure.runtime.ExpiredRequestException;
import com.socket.secure.runtime.InvalidRequestException;
import com.socket.secure.runtime.RepeatedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * All requests used to decrypt and authenticate token {@link Encrypted} controller methods
 */
@WebFilter
@Component
public final class SecureRequestFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SecureRequestFilter.class);
    private ApplicationEventPublisher publisher;
    private SecureProperties properties;
    private RepeatValidator validator;
    private HandlerMapping mapping;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest _request = (HttpServletRequest) request;
        // Get the handler that executes this URI
        HandlerMethod handler = this.getHandlerMethod(_request);
        if (handler == null) {
            chain.doFilter(request, response);
            return;
        }
        // Check the class tag first
        Encrypted anno = handler.getBeanType().getAnnotation(Encrypted.class);
        // check method tag if null
        if (anno == null) {
            anno = handler.getMethod().getAnnotation(Encrypted.class);
        }
        // Decrypt request
        if (anno != null) {
            try {
                SecureRequestWrapper wrapper = new SecureRequestWrapper(_request);
                // Decryption request
                wrapper.decryptRequset(anno.sign());
                // Expired request validation
                long time = wrapper.getTimestamp();
                if (validator.isExpired(time, properties.getLinkValidTime())) {
                    String template = "URL expired request interception [Request time: {}, System time: {}]";
                    throw new ExpiredRequestException(StrUtil.format(template, time, System.currentTimeMillis()));
                }
                // Repeat request validation
                if (validator.isRepeated(time, wrapper.sign())) {
                    String template = "URL repeated request interception [Request time: {}, System time: {}]";
                    throw new RepeatedRequestException(StrUtil.format(template, time, System.currentTimeMillis()));
                }
                // Signature verification
                if (!wrapper.matchSignature(properties.isVerifyFileSignature())) {
                    String template = "Signature verification failed: {}";
                    throw new InvalidRequestException(StrUtil.format(template, wrapper.sign()));
                }
                request = wrapper;
            } catch (InvalidRequestException | IllegalArgumentException e) {
                ((HttpServletResponse) response).setStatus(HttpStatus.BAD_REQUEST.value());
                this.pushEvent(_request, handler, e.getMessage());
                log.warn(e.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Gets the mapping of the current request URI {@link Method} <br>
     *
     * @param request {@link ServletRequest}
     * @return If no matching controller is found or no annotation is specified, null is returned
     */
    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            ServletRequestPathUtils.parseAndCache(request);
            HandlerExecutionChain chain = mapping.getHandler(request);
            if (chain != null) {
                return (HandlerMethod) chain.getHandler();
            }
        } catch (Exception e) {
            // ignore
        } finally {
            ServletRequestPathUtils.clearParsedRequestPath(request);
        }
        return null;
    }

    /**
     * Spring event push
     *
     * @param request {@link HttpServletRequest}
     * @param reason  Authentication failure reason
     */
    private void pushEvent(HttpServletRequest request, HandlerMethod hander, String reason) {
        InitiatorEvent event = new InitiatorEvent(publisher);
        UserAgent userAgent = UserAgentParser.parse(request.getHeader(Header.USER_AGENT.getValue()));
        event.setUserAgent(userAgent);
        event.setRemote(ServletUtil.getClientIP(request));
        event.setSession(request.getSession());
        event.setMethod(hander.getMethod());
        event.setController(hander.getBeanType());
        event.setReason(reason);
        publisher.publishEvent(event);
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setProperties(SecureProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setMapping(RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    @Autowired
    public void setValidator(RepeatValidator repeatValidator) {
        this.validator = repeatValidator;
    }
}
