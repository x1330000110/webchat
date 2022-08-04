package com.socket.secure.filter.anno;

import java.lang.annotation.*;

/**
 * API interface security transmission mark <br>
 * mark the method of this annotation, the spring will decrypt and verify the request before receiving the request. <br>
 * The filtering operation is implemented by {@linkplain com.socket.secure.filter.SecureRequestFilter SecureRequestFilter}. <br>
 * Dynamic interface decryption operations are not supported. <br>
 * Note: this annotation is not valid on non mapping methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {
    /**
     * Client request signature ID
     */
    String sign() default "sign";
}
