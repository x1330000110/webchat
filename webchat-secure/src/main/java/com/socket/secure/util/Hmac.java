package com.socket.secure.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.http.Header;
import com.socket.secure.constant.RequsetTemplate;
import com.socket.secure.exception.InvalidRequestException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
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
     * digest string
     *
     * @param request   {@link HttpServletRequest}
     * @param plaintext plaintext string
     * @return digest hex data
     */
    public String digestHex(HttpServletRequest request, String plaintext) {
        return digestHex(request, plaintext.getBytes());
    }

    /**
     * digest bytes
     *
     * @param request {@link HttpServletRequest}
     * @param bytes   bytes
     * @return digest hex string
     */
    public String digestHex(HttpServletRequest request, byte[] bytes) {
        return digestHex(getKey(request), bytes);
    }

    /**
     * digest bytes
     *
     * @param key   key
     * @param bytes bytes
     * @return digest hex string
     */
    public String digestHex(byte[] key, byte[] bytes) {
        try {
            Mac engine = Mac.getInstance(algorithm);
            engine.init(new SecretKeySpec(key, algorithm));
            engine.update(bytes, 0, bytes.length);
            return HexUtil.encodeHexStr(engine.doFinal());
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.HMAC_DIGEST_ERROR);
        }
    }

    /**
     * Hash UA using SHA1 and the current algorithm name
     */
    private byte[] getKey(HttpServletRequest request) {
        String ua = request.getHeader(Header.USER_AGENT.getValue());
        String key = SHA1.digestHex(algorithm.getBytes(), ua.getBytes());
        return key.getBytes();
    }
}
