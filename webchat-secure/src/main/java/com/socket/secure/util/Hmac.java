package com.socket.secure.util;

import com.socket.secure.runtime.InvalidRequestException;
import org.apache.tomcat.util.buf.HexUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class Hmac {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private final Mac engine;

    /**
     * build hmac digest
     *
     * @param algorithm algorithm
     * @param key       hmac string key
     */
    public Hmac(Algorithm algorithm, String key) {
        this(algorithm, key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * build hmac digest
     *
     * @param algorithm algorithm
     * @param key       hmac key
     */
    public Hmac(Algorithm algorithm, byte[] key) {
        try {
            this.engine = Mac.getInstance(algorithm.getAlgorithm());
            engine.init(new SecretKeySpec(key, algorithm.getAlgorithm()));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    /**
     * digest string
     *
     * @param str string
     * @return digeset hex string
     */
    public String digestHex(String str) {
        return digestHex(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * digest bytes
     *
     * @param bytes bytes
     * @return digeset hex string
     */
    public String digestHex(byte[] bytes) {
        return digestHex(new ByteArrayInputStream(bytes));
    }

    /**
     * digest InputStream
     *
     * @param stream InputStream
     * @return digeset hex string
     */
    public String digestHex(InputStream stream) {
        final int size = DEFAULT_BUFFER_SIZE;
        final byte[] buffer = new byte[size];
        try {
            int read = stream.read(buffer, 0, size);
            while (read > -1) {
                engine.update(buffer, 0, read);
                read = stream.read(buffer, 0, size);
            }
            return HexUtils.toHexString(engine.doFinal());
        } catch (IOException e) {
            throw new InvalidRequestException(e.getMessage());
        } finally {
            engine.reset();
        }
    }

    public enum Algorithm {
        MD5("HmacMD5"),
        SHA1("HmacSHA1"),
        SHA224("HmacSHA224"),
        SHA256("HmacSHA256"),
        SHA384("HmacSHA384"),
        SHA512("HmacSHA512");

        private final String algorithm;

        Algorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getAlgorithm() {
            return algorithm;
        }
    }
}
