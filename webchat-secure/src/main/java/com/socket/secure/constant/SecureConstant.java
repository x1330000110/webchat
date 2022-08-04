package com.socket.secure.constant;

import cn.hutool.core.codec.Base64;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

/**
 * safe-constant-pool
 */
public interface SecureConstant {
    /**
     * Supported Controller Flags
     */
    List<Class<? extends Annotation>> SUPPORT_REQUEST_ANNOS = Arrays.asList(RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class);
    /**
     * Camouflage picture Base64 format
     */
    byte[] CAMOUFLAGE_PICTURE_BYTES = Base64.decode("R0lGODlhAQABAIAAAP///wAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQEAAAAACwAAAAAAQABAAACAkQBADs=");
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
     * List of hexadecimal strings
     */
    String BASE_HEX = "0123456789abcdef";
    /**
     * array identifier
     */
    String ARRAY_MARK = "\u1000";
    /**
     * HMAC salt
     */
    String HMAC_SALT = "\u206a\u206e\u206b\u206d\u202a";
    /**
     * signature separator salt
     */
    String SIGN_SPLIT_SALT = "\u206a\u206e\u206b\u206d\u202a";
    /**
     * join salt
     */
    String JOIN_SALT = "\u206d\u202a\u206b\u206a\u206e";
    /**
     * public key sign salt
     */
    String PUBKEY_SIGN_SALT = "\u202a\u206d\u206b\u206a\u206e";
}
