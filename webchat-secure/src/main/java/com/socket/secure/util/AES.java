package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.socket.secure.constant.RequsetTemplate;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.exception.InvalidRequestException;
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
            throw new InvalidRequestException(RequsetTemplate.GENERATE_AESKEY_ERROR, e.getMessage());
        }
        generator.init(128);
        return HexUtil.encodeHexStr(generator.generateKey().getEncoded());
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
     * AES encryption (custom key)
     *
     * @param plaintext plaintext
     * @return ciphertext
     */
    public static String encrypt(String plaintext, String key) {
        Assert.notNull(key, RequsetTemplate.AESKEY_NOT_FOUNT, InvalidRequestException::new);
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key);
            return Base64.encode(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.AES_ENCRYPT_ERROR, plaintext);
        }
    }

    /**
     * Get the AES key exchanged in the current session, or null if the key does not exist
     */
    public static String getAesKey(HttpSession session) {
        return (String) session.getAttribute(SecureConstant.AESKEY);
    }

    private static Cipher getCipher(int mode, String key) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, key.length() / 2).forEach(i -> sb.append(Integer.toString(key.charAt(i), 16)));
        IvParameterSpec paramSpec = new IvParameterSpec(HexUtil.decodeHex(sb.toString()));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, keySpec, paramSpec);
        return cipher;
    }

    /**
     * AES decryption (using the current session's key)
     *
     * @param ciphertext &lt;ciphertext&gt;
     * @param session    {@linkplain HttpSession}
     * @return plaintext
     */
    public static String decrypt(String ciphertext, HttpSession session) {
        if (!StrUtil.isWrap(ciphertext, "<", ">")) {
            return ciphertext;
        }
        return decrypt(StrUtil.unWrap(ciphertext, "<", ">"), getAesKey(session));
    }

    /**
     * AES decryption (custom key)
     *
     * @param ciphertext ciphertext
     * @return plaintext
     */
    public static String decrypt(String ciphertext, String key) {
        Assert.notNull(key, RequsetTemplate.AESKEY_NOT_FOUNT, InvalidRequestException::new);
        if (!StringUtils.hasLength(ciphertext)) {
            return "";
        }
        // replace and convert
        byte[] bytes = Base64.decode(ciphertext);
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(bytes)).substring(RANDOM_PREFIX_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.AES_DECRYPT_ERROR, ciphertext);
        }
    }
}
