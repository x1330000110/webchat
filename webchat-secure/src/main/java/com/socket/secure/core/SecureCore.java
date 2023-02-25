package com.socket.secure.core;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import com.socket.secure.constant.RequsetTemplate;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.core.generator.SignatureGenerator;
import com.socket.secure.event.entity.KeyEvent;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.KeyPair;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Secure Transmission Service
 */
@Service
public class SecureCore {
    private ApplicationEventPublisher publisher;
    private SignatureGenerator generator;
    private SecureProperties properties;
    private HttpServletRequest request;
    private HttpSession session;

    /**
     * Client synchronization masquerade picture RSA public key
     *
     * @param response {@link HttpServletResponse}
     */
    public void syncPubkey(HttpServletResponse response) throws IOException {
        // Cache user identity
        IPHash.cacheIPHash(session, ServletUtil.getClientIP(request));
        session.setAttribute(SecureConstant.CONCURRENT_TIME, SystemClock.now());
        // Write camouflage picture
        ServletOutputStream stream = response.getOutputStream();
        stream.write(SecureConstant.CAMOUFLAGE_PICTURE_BYTES);
        // Write public key
        int count = properties.getDisguiseFilesCount();
        // Generate rsa keys
        KeyPair keyPair = RSA.generateKeyPair();
        byte[] pubkey = keyPair.getPublic().getEncoded();
        String signature = generator.generatePublicKeySignature(pubkey);
        String digest = Hmac.SHA1.digestHex(request, Base64.encode(pubkey));
        String base64Prikey = Base64.encode(keyPair.getPrivate().getEncoded());
        // Hmac sha1 save the corresponding private key
        session.setAttribute(digest.toUpperCase(), base64Prikey);
        // Write compressed file
        long headtime = SystemClock.now();
        try (ZipOutputStream zip = new ZipOutputStream(stream)) {
            int random = RandomUtil.randomInt(count);
            StringBuilder signs = new StringBuilder();
            // Build starts
            for (int i = 0; i <= count; i++) {
                boolean hit = i == random;
                byte[] bytes = hit ? pubkey : Randoms.randomBytes(pubkey.length);
                String name = hit ? signature : Randoms.randomHex(signature.length());
                ZipEntry entry = new ZipEntry(name);
                // Signature of the file name
                String sign = generator.generateFileNameSignature(name);
                signs.append(sign);
                entry.setComment(sign);
                zip.putNextEntry(entry);
                zip.write(bytes);
            }
            // SHA1 collection of file signatures
            String comment = Hmac.SHA1.digestHex(request, signs.toString());
            zip.setComment(comment);
        }
        // Write request header time (required)
        response.setDateHeader(Header.DATE.getValue(), headtime);
    }

    /**
     * Secure exchange of AES keys
     *
     * @param certificate Client encrypted segmented public key
     * @param key         get session server private key
     * @param digest      Client public key signature
     * @return Server encrypted AES key
     */
    public String syncAeskey(String certificate, String key, String digest) {
        // check ip hash
        boolean checkHash = IPHash.checkHash(session, ServletUtil.getClientIP(request));
        Assert.isTrue(checkHash, RequsetTemplate.IP_ADDRESS_MISMATCH, InvalidRequestException::new);
        // check time
        Long time = (Long) session.getAttribute(SecureConstant.CONCURRENT_TIME);
        boolean checkTime = time != null && SystemClock.now() - time <= SecureConstant.AES_KEY_EXCHANGE_MAXIMUM_TIME;
        Assert.isTrue(checkTime, RequsetTemplate.MAX_EXCHANGE_TIME, InvalidRequestException::new);
        // get private key
        String prikey = (String) session.getAttribute(key);
        Assert.notNull(prikey, RequsetTemplate.PRIVATE_KEY_NOT_FOUND, InvalidRequestException::new);
        // Decrypt client public key
        StringBuilder sb = new StringBuilder();
        for (String str : certificate.split(SecureConstant.ENCRYPT_PUBKEY_SPLIT)) {
            int length = str.length(), count = 0;
            char[] chars = new char[length >> 1];
            for (int i = 0; i < length; i += 2) {
                chars[count++] = (char) Integer.parseInt(str.substring(i, i + 2), 35);
            }
            sb.append(RSA.decrypt(String.valueOf(chars), prikey));
        }
        String pubkey = sb.toString();
        // Verify signature
        boolean equals = Hmac.SHA512.digestHex(request, pubkey).toUpperCase().equals(digest);
        Assert.isTrue(equals, RequsetTemplate.PUBLIC_KEY_SIGNATURE_MISMATCH, InvalidRequestException::new);
        session.removeAttribute(key);
        // Get/Generate AES key
        String aeskey = null;
        if (properties.isSameSessionWithOnlyKey()) {
            aeskey = AES.getAesKey(session);
        }
        if (aeskey == null) {
            aeskey = AES.generateAesKey();
            session.setAttribute(SecureConstant.AESKEY, aeskey);
        }
        // Push event
        publisher.publishEvent(new KeyEvent(publisher, session, aeskey));
        return RSA.encrypt(aeskey, pubkey).toUpperCase();
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setProperties(SecureProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setGenerator(SignatureGenerator generator) {
        this.generator = generator;
    }

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Autowired
    public void setSession(HttpSession session) {
        this.session = session;
    }
}
