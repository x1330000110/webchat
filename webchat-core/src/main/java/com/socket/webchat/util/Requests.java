package com.socket.webchat.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link HttpServletRequest}工具集
 *
 * @date 2022/2/9
 */
public class Requests {
    /**
     * Get the HttpServletRequest
     */
    public static HttpServletRequest get() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 检查指定标记是否存在
     *
     * @param name 属性名
     * @return 若存在返回true
     */
    public static boolean exist(String name) {
        return get(name) != null;
    }

    public static boolean notExist(String name) {
        return !exist(name);
    }

    /**
     * 获取会话储存的对象（强制转换）
     *
     * @param name 属性名
     * @return value
     * @throws ClassCastException 转换错误
     */
    public static <T> T get(String name) {
        Object obj = get().getAttribute(name);
        //noinspection unchecked
        return (T) obj;
    }

    /**
     * 设置属性标记
     *
     * @param name 属性
     */
    public static void set(String name) {
        set(name, new Object());
    }

    /**
     * 设置属性值
     *
     * @param name  属性
     * @param value 对象
     */
    public static void set(String name, Object value) {
        get().setAttribute(name, value);
    }
}
