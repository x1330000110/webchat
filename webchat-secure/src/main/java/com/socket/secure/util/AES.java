package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.runtime.InvalidRequestException;

import javax.servlet.http.HttpSession;
import java.util.stream.IntStream;

/**
 * AES decryption tool
 */
public class AES {
    private static final int RANDOM_PREFIX_LENGTH = 10;

    /**
     * Generate hex AES key
     *
     * @return AES key
     */
    public static String generateAesKey() {
        return HexUtil.encodeHexStr(KeyUtil.generateKey(AES.class.getSimpleName()).getEncoded());
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
        byte[] _key = StrUtil.bytes(key);
        cn.hutool.crypto.symmetric.AES aes = new cn.hutool.crypto.symmetric.AES(Mode.CBC, Padding.PKCS5Padding, _key, getIv(key));
        // Insert random number encryption to prevent reverse push
        byte[] ciphertext = aes.encrypt(RandomUtil.randomString(SecureConstant.BASE_HEX, RANDOM_PREFIX_LENGTH).concat(plaintext));
        return Base64.encode(ciphertext);
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
        if (StrUtil.isNotEmpty(ciphertext) && StrUtil.isWrap(ciphertext, '<', '>')) {
            byte[] _key = StrUtil.bytes(key);
            byte[] bytes = Base64.decode(StrUtil.unWrap(ciphertext, '<', '>'));
            try {
                byte[] decrypt = new cn.hutool.crypto.symmetric.AES(Mode.CBC, Padding.PKCS5Padding, _key, getIv(key)).decrypt(bytes);
                // 截取字符串
                return new String(decrypt).substring(RANDOM_PREFIX_LENGTH);
            } catch (Exception e) {
                throw new CryptoException("AES decrypt error");
            }
        }
        return ciphertext;
    }

    /**
     * Get AES offset
     */
    private static byte[] getIv(String key) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, key.length() / 2).forEach(i -> sb.append(Integer.toString(key.charAt(i), 16)));
        return HexUtil.decodeHex(sb.toString());
    }
}
