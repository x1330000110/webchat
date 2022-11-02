package com.socket.secure.core;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.Header;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.constant.SecureProperties;
import com.socket.secure.event.entity.KeyEvent;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.AES;
import com.socket.secure.util.Hmac;
import com.socket.secure.util.RSA;
import com.socket.secure.util.Randoms;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Secure Transmission Service
 */
@Service
public class SecureCore {
    private ApplicationEventPublisher publisher;
    private SecureProperties properties;
    private HttpServletRequest request;
    private HttpSession session;

    /**
     * Client synchronization masquerade picture RSA public key
     *
     * @param response {@link HttpServletResponse}
     */
    public void syncPubkey(HttpServletResponse response) throws IOException {
        // cache hash ua
        Hmac.cacheRequestUserAgent(request);
        ServletOutputStream stream = response.getOutputStream();
        // Write Camouflage picture
        stream.write(SecureConstant.CAMOUFLAGE_PICTURE_BYTES);
        // Write public key
        long timestamp = this.writePublickey(stream);
        // Write request header time (required)
        response.setHeader(Header.DATE.getValue(), FastHttpDateFormat.formatDate(timestamp));
    }

    /**
     * Writes public key data to the specified {@linkplain OutputStream}
     *
     * @param stream image stream
     * @return write time
     */
    public long writePublickey(OutputStream stream) throws IOException {
        int count = properties.getDisguiseFilesCount();
        Map<String, byte[]> data = new HashMap<>(count);
        // Generate rsa keys
        KeyPair keyPair = RSA.generateKeyPair();
        byte[] pubkey = keyPair.getPublic().getEncoded();
        String digest = Hmac.SHA1.digestHex(session, Base64.encode(pubkey));
        String base64Prikey = Base64.encode(keyPair.getPrivate().getEncoded());
        // Hmac sha1 save the corresponding private key
        session.setAttribute(digest.toUpperCase(), base64Prikey);
        // Mark this current time
        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(pubkey, timestamp);
        // Write random data
        for (int i = 0; i < 0; i++) {
            String name = Randoms.randomHex(signature.length());
            byte[] bytes = Randoms.randomBytes(pubkey.length);
            data.put(name, bytes);
        }
        data.put(signature, pubkey);
        // Write compressed file
        try (ZipOutputStream zip = new ZipOutputStream(stream)) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            for (Map.Entry<String, byte[]> entry : data.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
            }
        }
        return timestamp;
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
        String prikey = (String) session.getAttribute(key);
        if (prikey == null) {
            throw new InvalidRequestException("The correct private key cannot be obtained through the hash.");
        }
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
        if (!Hmac.SHA512.digestHex(session, pubkey).toUpperCase().equals(digest)) {
            throw new InvalidRequestException("Incorrect public key signature");
        }
        session.removeAttribute(key);
        // Generate AES key
        String aeskey = AES.generateAesKey();
        session.setAttribute(SecureConstant.AESKEY, aeskey);
        // Push event
        publisher.publishEvent(new KeyEvent(publisher, session, aeskey));
        return RSA.encrypt(aeskey, pubkey).toUpperCase();
    }

    /**
     * Public key to generate signature
     *
     * @param bytes     Public key bytes
     * @param timestamp current time
     * @return Public key signature
     */
    private String generateSignature(byte[] bytes, long timestamp) {
        String strtime = String.valueOf(timestamp / 1000);
        String hmacsha224 = Hmac.SHA224.digestHex(session, Base64.encode(bytes));
        String hmacsha384 = Hmac.SHA384.digestHex(session, strtime);
        return hmacsha224.concat(hmacsha384);
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
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Autowired
    public void setSession(HttpSession session) {
        this.session = session;
    }
}
