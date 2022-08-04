package com.socket.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import com.socket.secure.constant.SecureConstant;

import javax.servlet.http.HttpSession;
import java.security.KeyPair;

/**
 * RSA encryption tool
 */
public class RSA {
    /**
     * 生成RSA公钥，同时将私钥保存到会话中
     *
     * @return RSA公钥
     */
    public static byte[] generateRsaPublicKey(HttpSession session) {
        KeyPair keypair = KeyUtil.generateKeyPair(RSA.class.getSimpleName());
        session.setAttribute(SecureConstant.PRIVATE_KEY, Base64.encode(keypair.getPrivate().getEncoded()));
        return keypair.getPublic().getEncoded();
    }

    /**
     * RSA加密
     *
     * @param plaintext 明文
     * @param pubkey    公钥
     * @return 密文
     */
    public static String encrypt(String plaintext, String pubkey) {
        return new cn.hutool.crypto.asymmetric.RSA(null, pubkey).encryptHex(plaintext, KeyType.PublicKey);
    }

    /**
     * RSA解密
     *
     * @param ciphertext 密文
     * @param prikey     私钥
     * @return 明文
     */
    public static String decrypt(String ciphertext, String prikey) {
        try {
            return new cn.hutool.crypto.asymmetric.RSA(prikey, null).decryptStr(ciphertext, KeyType.PrivateKey);
        } catch (Exception e) {
            throw new CryptoException("RSA decrypt error");
        }
    }
}
