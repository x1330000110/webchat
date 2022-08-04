package com.socket.secure.filter;

import cn.hutool.crypto.CryptoException;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentParser;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.event.entity.InitiatorEvent;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.filter.validator.RepeatValidator;
import com.socket.secure.runtime.ExpiredRequestException;
import com.socket.secure.runtime.InvalidRequestException;
import com.socket.secure.runtime.RepeatedRequestException;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * All requests used to decrypt and authenticate token {@link Encrypted} controller methods
 */
@WebFilter
@Component
public final class SecureRequestFilter implements Filter {
    private ApplicationEventPublisher publisher;
    private SecureProperties properties;
    private RepeatValidator validator;
    private HandlerMapping mapping;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest _request = (HttpServletRequest) request;
        // Get the execution method of the corresponding URI mapping
        Encrypted annotation = this.getMethodAnnotation(_request, Encrypted.class);
        if (annotation != null) {
            // Parse request
            try {
                request = this.decrypt(_request, annotation.sign());
            } catch (InvalidRequestException | CryptoException | IllegalArgumentException exception) {
                this.setForbidden(response);
                this.pushEvent(_request, exception.getMessage());
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
    @SuppressWarnings("SameParameterValue")
    private <A extends Annotation> A getMethodAnnotation(HttpServletRequest request, Class<A> clazz) {
        try {
            ServletRequestPathUtils.parseAndCache(request);
            HandlerExecutionChain chain = mapping.getHandler(request);
            if (chain != null) {
                HandlerMethod handler = (HandlerMethod) chain.getHandler();
                A annotation = handler.getMethodAnnotation(clazz);
                if (annotation != null && isSupport(handler)) {
                    return annotation;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ServletRequestPathUtils.clearParsedRequestPath(request);
        }
        return null;
    }

    /**
     * Decrypt and verify the security of this request
     *
     * @param request  {@link HttpServletRequest}
     * @param signName Request signature key name
     * @return decrypted request
     */
    public HttpServletRequest decrypt(HttpServletRequest request, String signName) throws IOException, ServletException {
        SecureRequestWrapper wrapper = new SecureRequestWrapper(request);
        // Decryption request
        wrapper.decryptData(signName);
        // Expired request validation
        long time = wrapper.getTimestamp();
        if (validator.isExpired(time, properties.getLinkValidTime())) {
            throw new ExpiredRequestException("URL expired request interception");
        }
        // Repeat request validation
        time = properties.isExactRequestTime() ? time : time / 1000;
        if (validator.isRepeated(time, wrapper.sign())) {
            throw new RepeatedRequestException("URL repeated request interception");
        }
        // Signature verification
        if (!wrapper.matchSignature(properties.isVerifyFileSignature())) {
            throw new InvalidRequestException("Signature verification failed");
        }
        return wrapper;
    }

    /**
     * Check if this method is protected by encryption
     */
    private boolean isSupport(HandlerMethod handler) {
        return SecureConstant.SUPPORT_REQUEST_ANNOS.stream().anyMatch(e -> handler.getMethodAnnotation(e) != null);
    }

    /**
     * Set 403 status code
     *
     * @param response {@link HttpServletResponse}
     */
    private void setForbidden(ServletResponse response) {
        ((HttpServletResponse) response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Spring event push
     *
     * @param request {@link HttpServletRequest}
     * @param reason  Authentication failure reason
     */
    private void pushEvent(HttpServletRequest request, String reason) {
        InitiatorEvent event = new InitiatorEvent(publisher);
        UserAgent userAgent = UserAgentParser.parse(request.getHeader(Header.USER_AGENT.getValue()));
        event.setUserAgent(userAgent);
        event.setRemote(ServletUtil.getClientIP(request));
        event.setSessionId(request.getSession().getId());
        event.setDescription(reason);
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
    public void setValidator(RepeatValidator validator) {
        this.validator = validator;
    }
}
