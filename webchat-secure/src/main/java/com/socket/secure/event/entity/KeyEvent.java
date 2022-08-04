package com.socket.secure.event.entity;

import org.springframework.context.ApplicationEvent;

import javax.servlet.http.HttpSession;

/**
 * Key event entity encapsulation
 */
public class KeyEvent extends ApplicationEvent {
    private final HttpSession session;
    private final String key;

    public KeyEvent(Object source, HttpSession session, String key) {
        super(source);
        this.session = session;
        this.key = key;
    }

    public HttpSession getSession() {
        return session;
    }

    public String getKey() {
        return key;
    }
}