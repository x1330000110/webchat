package com.socket.client.util;

import com.socket.client.exception.SocketException;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.enums.MessageType;

public class Assert {
    public static void notGuest(WsUser target, Callback callback, MessageType type) {
        if (target.isGuest()) {
            throw new SocketException(callback.format(), type);
        }
    }

    public static void isAdmin(WsUser t1, WsUser t2, Callback callback, MessageType type) {
        if (t1.isAdmin() && t2.isOwner()) {
            throw new SocketException(callback.format(), type);
        }
    }

    public static void isOwner(WsUser target, Callback callback, MessageType type) {
        if (!target.isOwner()) {
            throw new SocketException(callback.format(), type);
        }
    }

    public static void isTrue(boolean b, Callback callback, MessageType type) {
        if (!b) {
            throw new SocketException(callback.format(), type);
        }
    }

    public static void isFalse(boolean b, Callback callback, MessageType type) {
        if (b) {
            throw new SocketException(callback.format(), type);
        }
    }

    public static void notNull(WsUser target, Callback callback, MessageType type) {
        if (target == null) {
            throw new SocketException(callback.format(), type);
        }
    }
}
