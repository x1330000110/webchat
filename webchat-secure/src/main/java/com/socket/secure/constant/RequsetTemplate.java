package com.socket.secure.constant;

import cn.hutool.core.util.StrUtil;

public enum RequsetTemplate {
    EXPIRED_REQUEST("URL expired request interception [Request time: {}, System time: {}]"),
    REPEATED_REQUEST("URL repeated request interception [Request time: {}, System time: {}]"),
    INVALID_REQUEST_SIGNATURE("Invalid requset signature: {}"),
    AESKEY_NOT_FOUNT("AES key for current session not found"),
    IP_ADDRESS_MISMATCH("IP address mismatch"),
    MAX_EXCHANGE_TIME("The time interval between two requests exceeds the maximum limit"),
    PRIVATE_KEY_NOT_FOUND("The correct private key cannot be obtained through the hash"),
    PUBLIC_KEY_SIGNATURE_MISMATCH("Incorrect public key signature"),
    AES_ENCRYPT_ERROR("AES encrypt error: {}"),
    AES_DECRYPT_ERROR("AES decrypt error: {}"),
    RSA_ENCRYPT_ERROR("RSA encrypt error: {}"),
    RSA_DECRYPT_ERROR("RSA decrypt error: {}"),
    HMAC_DIGEST_ERROR("HMAC digest error: {}"),
    GENERATE_AESKEY_ERROR("Generate AES key error: {}"),
    GENERATE_RSAKEY_ERROR("Generate RSA key error: {}"),
    INVALID_HEADER_SIGNATURE("Invalid request header signature"),
    REQUSET_SIGNATURE_NOT_FOUNT("The signature for the current request cannot be found");

    private final String template;

    RequsetTemplate(String template) {
        this.template = template;
    }

    public String format(Object... objs) {
        return StrUtil.format(template, objs);
    }
}
