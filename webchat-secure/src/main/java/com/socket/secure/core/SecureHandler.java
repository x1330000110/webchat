package com.socket.secure.core;

import cn.hutool.crypto.CryptoException;
import com.socket.secure.runtime.InvalidRequestException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(1)
@RestController
@RestControllerAdvice
public class SecureHandler {
    private final SecureCore core;

    SecureHandler(SecureCore core) {
        this.core = core;
    }

    @GetMapping("/secure")
    private void syncPubkey(HttpServletResponse response) throws IOException {
        core.syncPubkey(response);
    }

    @PostMapping("/secure")
    private String syncAeskey(@RequestBody String certificate, HttpServletRequest request) {
        return core.syncAeskey(certificate, request.getHeader("SHA-512"));
    }

    @ExceptionHandler({InvalidRequestException.class, CryptoException.class})
    private ResponseEntity<Object> isValidationFailedException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
}
