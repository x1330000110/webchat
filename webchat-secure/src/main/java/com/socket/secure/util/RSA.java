package com.socket.secure.util;

import com.socket.secure.constant.SecureConstant;
import com.socket.secure.runtime.InvalidRequestException;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;
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
     * Generate RSA public key while saving private key to session
     *
     * @return rsa Public Key
     */
    public static byte[] generateRsaPublicKey(HttpSession session) {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        generator.initialize(1024);
        KeyPair keypair = generator.generateKeyPair();
        session.setAttribute(SecureConstant.PRIVATE_KEY, Base64Utils.encodeToString(keypair.getPrivate().getEncoded()));
        return keypair.getPublic().getEncoded();
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
            KeySpec keySpec = new X509EncodedKeySpec(Base64Utils.decodeFromString(pubkey));
            Key key = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return HexUtils.toHexString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException("RSA encrypt failure: " + e.getMessage());
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
            KeySpec keySpec = new PKCS8EncodedKeySpec(Base64Utils.decodeFromString(prikey));
            Key key = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64Utils.decodeFromUrlSafeString(ciphertext)));
        } catch (GeneralSecurityException e) {
            throw new InvalidRequestException("RSA decrypt failure: " + e.getMessage());
        }
    }
}
