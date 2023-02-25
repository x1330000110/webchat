package com.socket.server.custom.filter.anno;

import java.lang.annotation.*;

/**
 * 公共服务接口标记（供服务之间调用，用户无法访问）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenApi {
}
