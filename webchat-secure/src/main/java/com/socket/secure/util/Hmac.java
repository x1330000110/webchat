package com.socket.secure.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.exception.InvalidRequestException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.GeneralSecurityException;

public enum Hmac {
    MD5("HmacMD5"),
    SHA1("HmacSHA1"),
    SHA224("HmacSHA224"),
    SHA256("HmacSHA256"),
    SHA384("HmacSHA384"),
    SHA512("HmacSHA512");
    /**
     * algorithm
     */
    private final String algorithm;

    /**
     * build hmac digest
     *
     * @param algorithm algorithm
     */
    Hmac(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Cache the current Resqust user-agent to the Session
     */
    public static void cacheRequestUserAgent(HttpServletRequest request) {
        String header = request.getHeader(Header.USER_AGENT.getValue());
        String hash = SecureUtil.sha1().digestHex(header);
        request.getSession().setAttribute(SecureConstant.DIGEST_UA, hash);
    }

    /**
     * digest string
     *
     * @param session {@link HttpSession}
     * @param data    data
     * @return digeset hex data
     */
    public String digestHex(HttpSession session, String data) {
        return digestHex(session, data.getBytes());
    }

    /**
     * digest bytes
     *
     * @param session {@link HttpSession}
     * @param data    bytes
     * @return digeset hex string
     */
    public String digestHex(HttpSession session, byte[] data) {
        try {
            Mac engine = Mac.getInstance(algorithm);
            engine.init(new SecretKeySpec(getKey(session), algorithm));
            engine.update(data, 0, data.length);
            return HexUtil.encodeHexStr(engine.doFinal());
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    /**
     * Get the Hmac key for this session.
     */
    private byte[] getKey(HttpSession session) {
        Object key = session.getAttribute(SecureConstant.DIGEST_UA);
        if (key == null) {
            throw new InvalidRequestException("The current session does not contain user-agent hash data");
        }
        return String.valueOf(key).getBytes();
    }
}
