package com.socket.secure.util;

import java.util.concurrent.ThreadLocalRandom;

public class Randoms {
    private static final String BASE_HEX = "0123456789abcdef";

    public static byte[] randomBytes(int length) {
        ThreadLocalRandom current = ThreadLocalRandom.current();
        byte[] bytes = new byte[length];
        current.nextBytes(bytes);
        return bytes;
    }

    public static String randomHex(int length) {
        ThreadLocalRandom current = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        current.ints(length).forEach(i -> sb.append(BASE_HEX.charAt(i)));
        return sb.toString();
    }
}
