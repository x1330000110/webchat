package com.socket.secure.core;

import com.socket.secure.constant.RequsetTemplate;
import com.socket.secure.exception.InvalidRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Secure Transmission Handler
 */
@RestController
public class SecureHandler {
    private final SecureCore secureCore;

    SecureHandler(SecureCore secureCore) {
        this.secureCore = secureCore;
    }

    @GetMapping("/secure")
    private void syncPubkey(HttpServletResponse response) throws IOException {
        secureCore.syncPubkey(response);
    }

    @PostMapping("/secure")
    private String syncAeskey(@RequestBody String certificate, HttpServletRequest request) {
        String signature = request.getHeader("signature");
        if (signature != null && signature.length() == 168) {
            return secureCore.syncAeskey(certificate, signature.substring(0, 40), signature.substring(40));
        }
        throw new InvalidRequestException(RequsetTemplate.INVALID_HEADER_SIGNATURE);
    }
}
