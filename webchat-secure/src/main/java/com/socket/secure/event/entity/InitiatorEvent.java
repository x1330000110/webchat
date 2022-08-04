package com.socket.secure.event.entity;

import cn.hutool.http.useragent.UserAgent;
import org.springframework.context.ApplicationEvent;

/**
 * Record request originator information
 *
 * @date 2021/11/4
 */
public class InitiatorEvent extends ApplicationEvent {
    /**
     * Browser information
     */
    private UserAgent userAgent;
    /**
     * IP address
     */
    private String remote;
    /**
     * Session ID
     */
    private String sessionId;
    /**
     * Interception reason
     */
    private String description;

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

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
