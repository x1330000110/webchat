package com.socket.secure.constant;

import cn.hutool.core.codec.Base64;

/**
 * safe-constant-pool
 */
public interface SecureConstant {
    /**
     * Camouflage picture Base64 format
     */
    byte[] CAMOUFLAGE_PICTURE_BYTES = Base64.decode("R0lGODlhAQABAIAAAP///wAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQEAAAAACwAAAAAAQABAAACAkQBADs=");
    /**
     * Session AES key ID (this key is used for data encryption)
     */
    String AESKEY = "AESKEY";
    /**
     * Hash user-agent
     */
    String DIGEST_UA = "DIGEST_UA";
    /**
     * Hash requset-ip
     */
    String DIGEST_IP = "DIGEST_IP";
    /**
     * The time when the key exchange was initiated for the first time.
     */
    String CONCURRENT_TIME = "CONCURRENT_TIME";
    /**
     * Client encryption public key separator
     */
    String ENCRYPT_PUBKEY_SPLIT = "Z";
    /**
     * array identifier
     */
    String ARRAY_MARK = "\u1000";
    /**
     * AES key maximum exchange time interval (unit: ms)
     */
    long AES_KEY_EXCHANGE_MAXIMUM_TIME = 60000;
}
