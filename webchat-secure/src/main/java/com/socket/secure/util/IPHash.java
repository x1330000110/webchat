package com.socket.secure.util;

import cn.hutool.crypto.SecureUtil;
import com.socket.secure.constant.SecureConstant;

import javax.servlet.http.HttpSession;

public class IPHash {
    public static boolean checkHash(HttpSession session, String ip) {
        return SecureUtil.sha1().digestHex(ip).equals(session.getAttribute(SecureConstant.DIGEST_IP));
    }

    public static void cacheIPHash(HttpSession session, String ip) {
        session.setAttribute(SecureConstant.DIGEST_IP, SecureUtil.sha1().digest(ip));
    }
}
