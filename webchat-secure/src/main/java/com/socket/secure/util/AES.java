package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.exception.InvalidRequestException;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;

/**
 * AES decryption tool
 */
public class AES {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int RANDOM_PREFIX_LENGTH = 10;

    /**
     * Generate hex AES key
     *
     * @return AES key
     */
    public static String generateAesKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        generator.init(128);
        return HexUtils.toHexString(generator.generateKey().getEncoded());
    }

    /**
     * Get the AES key exchanged in the current session, or null if the key does not exist
     */
    public static String getAesKey(HttpSession session) {
        return (String) session.getAttribute(SecureConstant.AESKEY);
    }

    /**
     * AES encryption (using the current session's key)
     *
     * @param plaintext plaintext
     * @param session   {@linkplain HttpSession}
     * @return ciphertext
     */
    public static String encrypt(String plaintext, HttpSession session) {
        return encrypt(plaintext, getAesKey(session));
    }

    /**
     * AES decryption (using the current session's key)
     *
     * @param ciphertext ciphertext
     * @param session    {@linkplain HttpSession}
     * @return plaintext
     */
    public static String decrypt(String ciphertext, HttpSession session) {
        return decrypt(ciphertext, getAesKey(session));
    }

    /**
     * AES encryption (custom key)
     *
     * @param plaintext plaintext
     * @return ciphertext
     */
    public static String encrypt(String plaintext, String key) {
        if (key == null) {
            throw new InvalidRequestException("AES key is invalid");
        }
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key);
            return Base64.encode(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException("AES encrypt failure: " + e.getMessage());
        }
    }

    /**
     * AES decryption (custom key)
     *
     * @param ciphertext ciphertext
     * @return plaintext
     */
    public static String decrypt(String ciphertext, String key) {
        if (key == null) {
            throw new InvalidRequestException("AES key is invalid");
        }
        if (!StringUtils.hasLength(ciphertext)) {
            return "";
        }
        // check mark
        if (!(ciphertext.startsWith("<") && ciphertext.endsWith(">"))) {
            return ciphertext;
        }
        byte[] bytes = Base64.decode(ciphertext.substring(1, ciphertext.length() - 1));
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(bytes)).substring(RANDOM_PREFIX_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException("AES decrypt failure: " + e.getMessage());
        }
    }

    private static Cipher getCipher(int mode, String key) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, key.length() / 2).forEach(i -> sb.append(Integer.toString(key.charAt(i), 16)));
        IvParameterSpec paramSpec = new IvParameterSpec(HexUtils.fromHexString(sb.toString()));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, keySpec, paramSpec);
        return cipher;
    }
}
