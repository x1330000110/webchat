package com.socket.secure.core.generator.impl;

import cn.hutool.core.codec.Base64;
import com.socket.secure.core.generator.SignatureGenerator;
import com.socket.secure.util.Hmac;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 签名生成器实现
 */
@Component
public class SignatureGeneratorImpl implements SignatureGenerator {
    private HttpServletRequest request;

    @Override
    public String generatePublicKeySignature(byte[] bytes) {
        return Hmac.SHA384.digestHex(request, Base64.encode(bytes));
    }

    @Override
    public String generateFileNameSignature(String filename) {
        return Hmac.SHA224.digestHex(request, filename);
    }

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
