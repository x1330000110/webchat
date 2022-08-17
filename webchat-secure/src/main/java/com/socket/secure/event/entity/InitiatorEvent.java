package com.socket.secure.event.entity;

import cn.hutool.http.useragent.UserAgent;
import org.springframework.context.ApplicationEvent;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;

/**
 * Record request originator information
 */
public class InitiatorEvent extends ApplicationEvent {
    /**
     * target controller
     */
    private Class<?> controller;
    /**
     * method for handling URI
     */
    private Method method;
    /**
     * IP address
     */
    private String remote;
    /**
     * Session ID
     */
    private HttpSession session;
    /**
     * Interception reason
     */
    private String reason;
    /**
     * Browser information
     */
    private UserAgent userAgent;

    public InitiatorEvent(Object source) {
        super(source);
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public String getRemote() {
        return remote;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Class<?> getController() {
        return controller;
    }

    public void setController(Class<?> type) {
        this.controller = type;
    }
}
