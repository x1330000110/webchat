package com.socket.secure.util;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Function;

/**
 * RSA key adapter enum
 */
public enum RsaKey {
    PUBLIC_KEY(X509EncodedKeySpec::new) {
        @Override
        public PublicKey generatePublic(byte[] key) throws GeneralSecurityException {
            return KeyFactory.getInstance("RSA").generatePublic(lambda.apply(key));
        }
    },
    PRIVATE_KEY(PKCS8EncodedKeySpec::new) {
        @Override
        public PrivateKey generatePrivate(byte[] key) throws GeneralSecurityException {
            return KeyFactory.getInstance("RSA").generatePrivate(lambda.apply(key));
        }
    };

    final Function<byte[], EncodedKeySpec> lambda;

    RsaKey(Function<byte[], EncodedKeySpec> lambda) {
        this.lambda = lambda;
    }

    PublicKey generatePublic(byte[] key) throws GeneralSecurityException {
        return null;
    }

    PrivateKey generatePrivate(byte[] key) throws GeneralSecurityException {
        return null;
    }
}