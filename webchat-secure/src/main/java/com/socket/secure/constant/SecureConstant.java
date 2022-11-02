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
     * Client encryption public key separator
     */
    String ENCRYPT_PUBKEY_SPLIT = "Z";
    /**
     * array identifier
     */
    String ARRAY_MARK = "\u1000";
}
