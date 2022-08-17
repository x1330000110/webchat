package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.CryptoException;
import com.socket.secure.constant.SecureConstant;

import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.*;

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
            throw new CryptoException(e.getMessage());
        }
        generator.initialize(1024);
        KeyPair keypair = generator.generateKeyPair();
        session.setAttribute(SecureConstant.PRIVATE_KEY, Base64.encode(keypair.getPrivate().getEncoded()));
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
            PublicKey key = RsaKey.PUBLIC_KEY.generatePublic(Base64.decode(pubkey));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return HexUtil.encodeHexStr(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA encrypt failure: " + e.getMessage());
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
            PrivateKey key = RsaKey.PRIVATE_KEY.generatePrivate(Base64.decode(prikey));
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(ciphertext)));
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA decrypt failure: " + e.getMessage());
        }
    }
}
