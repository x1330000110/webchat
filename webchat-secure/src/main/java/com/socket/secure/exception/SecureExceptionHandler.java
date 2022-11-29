package com.socket.secure.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice
public class SecureExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(SecureExceptionHandler.class);

    @ExceptionHandler(InvalidRequestException.class)
    private ResponseEntity<Object> isInvalidRequestException(Exception e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
}
