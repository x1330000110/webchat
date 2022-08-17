package com.socket.secure.filter.anno;

import java.lang.annotation.*;

/**
 * API interface security transmission mark <br>
 * Mark this annotation, the spring will decrypt and verify the request before receiving the request. <br>
 * Note: This annotation will strictly check the mark position.
 * Currently, it can only be marked on the class of @Controller or @RestController,
 * can also be marked on @RequestMapping or derived annotated methods.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {
    /**
     * Client request signature ID
     */
    String sign() default "sign";
}
