package com.socket.secure.util;

import cn.hutool.crypto.SecureUtil;
import com.socket.secure.constant.SecureConstant;

import javax.servlet.http.HttpSession;

/**
 * IP哈希校验工具
 */
public class IPHash {
    /**
     * 对比IP地址
     */
    public static boolean checkHash(HttpSession session, String ip) {
        return SecureUtil.sha1().digestHex(ip).equals(session.getAttribute(SecureConstant.DIGEST_IP));
    }

    /**
     * 缓存IP地址
     */
    public static void cacheIPHash(HttpSession session, String ip) {
        session.setAttribute(SecureConstant.DIGEST_IP, SecureUtil.sha1().digestHex(ip));
    }
}
