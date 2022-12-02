package com.socket.webchat.request.anno;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

/**
 * 接口代理标记
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@RequestMapping
public @interface FeignProxy {
    String[] value() default {};

    String[] path() default {};

    RequestMethod[] method() default {};
}
