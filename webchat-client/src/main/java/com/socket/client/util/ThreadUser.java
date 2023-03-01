package com.socket.client.util;

import com.socket.core.model.AuthUser;

/**
 * 线程用户工具
 */
public class ThreadUser {
    private static final ThreadLocal<AuthUser> local = new ThreadLocal<>();

    public static void set(AuthUser uid) {
        local.set(uid);
    }

    public static AuthUser get() {
        return local.get();
    }

    public static void remove() {
        local.remove();
    }
}
