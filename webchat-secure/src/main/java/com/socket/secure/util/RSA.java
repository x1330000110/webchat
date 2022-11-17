package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import com.socket.secure.constant.RequsetTemplate;
import com.socket.secure.exception.InvalidRequestException;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA encryption tool
 */
public class RSA {
    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * Generate RSA KeyPair
     *
     * @return KeyPair
     */
    public static KeyPair generateKeyPair() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidRequestException(RequsetTemplate.GENERATE_RSAKEY_ERROR, e.getMessage());
        }
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    /**
     * RSA encrypt
     *
     * @param plaintext plaintext
     * @param pubkey    public key
     * @return ciphertext
     */
    public static String encrypt(String plaintext, String pubkey) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            KeySpec keySpec = new X509EncodedKeySpec(Base64.decode(pubkey));
            Key key = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return HexUtil.encodeHexStr(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.RSA_ENCRYPT_ERROR, plaintext);
        }
    }

    /**
     * RSA decrypt
     *
     * @param ciphertext ciphertext
     * @param prikey     private key
     * @return plaintext
     */
    public static String decrypt(String ciphertext, String prikey) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            KeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(prikey));
            Key key = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(ciphertext)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException(RequsetTemplate.RSA_DECRYPT_ERROR, ciphertext);
        }
    }
}
