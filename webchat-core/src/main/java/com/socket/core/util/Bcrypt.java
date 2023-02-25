package com.socket.core.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * Bcrypt散列库
 */
public class Bcrypt {
    private static final String DEFAULT_BCRYPT_PREFIX = "$2a$10$";

    /**
     * 散列原文（不可逆）
     *
     * @param text 原文
     * @return 散列码
     */
    public static String digest(String text) {
        return BCrypt.hashpw(text).replace(DEFAULT_BCRYPT_PREFIX, "");
    }

    /**
     * 验证原文与散列值是否匹配
     *
     * @param text   原文
     * @param hashed 散列码
     * @return 是否匹配
     */
    public static boolean verify(String text, String hashed) {
        return BCrypt.checkpw(text, DEFAULT_BCRYPT_PREFIX + hashed);
    }
}
