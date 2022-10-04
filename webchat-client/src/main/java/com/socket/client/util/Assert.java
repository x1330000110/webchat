package com.socket.client.util;

import com.socket.client.exception.SocketException;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.enums.MessageType;

public class Assert {
    public static void isNull(Object obj, Callback callback, MessageType type, Object... objs) {
        if (obj != null) {
            throw new SocketException(callback.of(objs), type);
        }
    }

    public static void notNull(Object obj, Callback callback, MessageType type, Object... objs) {
        if (obj == null) {
            throw new SocketException(callback.of(objs), type);
        }
    }

    public static void isTrue(boolean b, Callback callback, MessageType type, Object... objs) {
        if (!b) {
            throw new SocketException(callback.of(objs), type);
        }
    }

    public static void isFalse(boolean b, Callback callback, MessageType type, Object... objs) {
        if (b) {
            throw new SocketException(callback.of(objs), type);
        }
    }
}
