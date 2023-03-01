package com.socket.server.util.servlet;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServletResponse}工具集
 *
 * @date 2022/2/9
 */
public class Response {
    /**
     * 设置属性值
     *
     * @param name  头名称
     * @param value 值
     */
    public static void setHeader(String name, String value) {
        get().setHeader(name, value);
    }

    /**
     * Get the HttpServletRequest
     */
    public static HttpServletResponse get() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        return ((ServletRequestAttributes) requestAttributes).getResponse();
    }

    /**
     * 设置响应码
     *
     * @param status 响应码
     */
    public static void setStatus(int status) {
        get().setStatus(status);
    }
}
