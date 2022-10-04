package com.socket.secure.core;

import com.socket.secure.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SecureHandler.class);
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

    @ExceptionHandler(InvalidRequestException.class)
    private ResponseEntity<Object> isInvalidRequestException(Exception e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
}
