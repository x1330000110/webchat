package com.socket.secure.util;

import com.socket.secure.runtime.InvalidRequestException;
import org.apache.tomcat.util.buf.HexUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
     * @param key  hmac key
     * @param data data
     * @return digeset hex data
     */
    public String digestHex(String key, String data) {
        return digestHex(key, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * digest string
     *
     * @param key  hmac key
     * @param data data
     * @return digeset hex data
     */
    public String digestHex(byte[] key, String data) {
        return digestHex(key, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * digest bytes
     *
     * @param key  hmac key
     * @param data data
     * @return digeset hex data
     */
    public String digestHex(String key, byte[] data) {
        return digestHex(key.getBytes(StandardCharsets.UTF_8), data);
    }

    /**
     * digest bytes
     *
     * @param key  hmac key
     * @param data bytes
     * @return digeset hex string
     */
    public String digestHex(byte[] key, byte[] data) {
        try {
            Mac engine = Mac.getInstance(algorithm);
            engine.init(new SecretKeySpec(key, algorithm));
            engine.update(data, 0, data.length);
            return HexUtils.toHexString(engine.doFinal());
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
