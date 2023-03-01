package com.socket.client.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.stream.IntStream;

/**
 * 来自webchat-secure#AES
 */
public class AES {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int RANDOM_PREFIX_LENGTH = 10;

    public static String encrypt(String plaintext, String key) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key);
            return Base64.encode(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            return null;
        }
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

    public static String decrypt(String ciphertext, String key) {
        if (!StringUtils.hasLength(ciphertext)) {
            return "";
        }
        // replace and convert
        byte[] bytes = Base64.decode(ciphertext);
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(bytes)).substring(RANDOM_PREFIX_LENGTH);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }
}
