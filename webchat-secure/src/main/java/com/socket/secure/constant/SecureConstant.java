package com.socket.secure.constant;

import org.springframework.util.Base64Utils;

/**
 * safe-constant-pool
 */
public interface SecureConstant {
    /**
     * Camouflage picture Base64 format
     */
    byte[] CAMOUFLAGE_PICTURE_BYTES = Base64Utils.decodeFromString("R0lGODlhAQABAIAAAP///wAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQEAAAAACwAAAAAAQABAAACAkQBADs=");
    /**
     * Session private key ID
     */
    String PRIVATE_KEY = "PRIVATE_KEY";
    /**
     * Session AES key ID (this key is used for data encryption)
     */
    String AESKEY = "AESKEY";
    /**
     * Client encryption public key separator
     */
    String ENCRYPT_PUBKEY_SPLIT = "Z";
    /**
     * array identifier
     */
    String ARRAY_MARK = "\u1000";
    /**
     * Hmac salt
     */
    byte[] HMAC_SALT = {-30, -127, -86, -30, -127, -82, -30, -127, -85, -30, -127, -83, -30, -128, -86};
    /**
     * signature separator salt
     */
    char[] SIGN_SPLIT_SALT = {8298, 8302, 8299, 8301, 8234};
    /**
     * join salt
     */
    char[] JOIN_SALT = {8301, 8234, 8299, 8298, 8302};
    /**
     * public key sign salt
     */
    byte[] PUBKEY_SIGN_SALT = {-30, -128, -86, -30, -127, -83, -30, -127, -85, -30, -127, -86, -30, -127, -82};
}
