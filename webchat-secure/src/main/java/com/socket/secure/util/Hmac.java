package com.socket.secure.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import com.socket.secure.constant.RequsetTemplate;
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
     * Cache global hmac key
     */
    public static void cacheGlobalHmacKey(HttpServletRequest request) {
        String ua = request.getHeader(Header.USER_AGENT.getValue());
        String digest = SecureUtil.sha1().digestHex(ua);
        request.getSession().setAttribute(SecureConstant.DIGEST_UA, digest);
    }

    /**
     * digest string
     *
     * @param session {@link HttpSession}
     * @param data    data
     * @return digest hex data
     */
    public String digestHex(HttpSession session, String data) {
        return digestHex(session, data.getBytes());
    }

    /**
     * digest bytes
     *
     * @param session {@link HttpSession}
     * @param data    bytes
     * @return digest hex string
     */
    public String digestHex(HttpSession session, byte[] data) {
        try {
            Mac engine = Mac.getInstance(algorithm);
            engine.init(new SecretKeySpec(getKey(session), algorithm));
            engine.update(data, 0, data.length);
            return HexUtil.encodeHexStr(engine.doFinal());
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.HMAC_DIGEST_ERROR);
        }
    }

    /**
     * Get the Hmac key for this session.
     */
    private byte[] getKey(HttpSession session) {
        String key = (String) session.getAttribute(SecureConstant.DIGEST_UA);
        Assert.notNull(key, RequsetTemplate.DIGEST_UA_NOT_FOUNT, InvalidRequestException::new);
        return key.getBytes();
    }
}
