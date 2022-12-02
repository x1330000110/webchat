package com.socket.client.util;

import com.socket.client.exception.SocketException;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.MessageEnum;

public class Assert {
    public static void isAdmin(WsUser t1, WsUser t2, String msg) {
        if (t1.isAdmin() && t2.isOwner()) {
            throw new SocketException(msg, MessageEnum.DANGER);
        }
    }

    public static void isOwner(WsUser target, String msg) {
        if (!target.isOwner()) {
            throw new SocketException(msg, MessageEnum.DANGER);
        }
    }

    public static void isTrue(boolean b, String msg) {
        if (!b) {
            throw new SocketException(msg, MessageEnum.DANGER);
        }
    }

    public static void isFalse(boolean b, String msg) {
        if (b) {
            throw new SocketException(msg, MessageEnum.DANGER);
        }
    }

    public static void notNull(WsUser target, String msg) {
        if (target == null) {
            throw new SocketException(msg, MessageEnum.DANGER);
        }
    }
}
