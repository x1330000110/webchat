package com.socket.server.request.anno;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

/**
 * 微信登录重定向标记，标记此注解同时开启跨域 <br>
 * 此注解等同于{@linkplain GetMapping @GetMapping} + {@linkplain CrossOrigin @CrossOrigin},
 * 只能在 {@linkplain Controller @Controller} 以及派生注解的类下可以被扫描到
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CrossOrigin
@RequestMapping(method = RequestMethod.GET)
public @interface WeChatRedirect {
    /**
     * 跳转映射地址
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] value() default {};
}
