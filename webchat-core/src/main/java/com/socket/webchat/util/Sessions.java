package com.socket.webchat.util;

import javax.servlet.http.HttpSession;
import java.util.Arrays;

/**
 * {@link HttpSession}工具集
 *
 * @date 2022/2/9
 */
public class Sessions {
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
     * Get the HttpSession
     */
    public static HttpSession get() {
        return Requests.get().getSession();
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

    /**
     * 移除指定属性
     *
     * @param name 属性名
     * @return 若成功移除返回true
     */
    public static boolean removeIf(String name) {
        HttpSession session = get();
        if (session.getAttribute(name) == null) {
            return false;
        }
        session.removeAttribute(name);
        return true;
    }

    /**
     * 移除多个属性
     *
     * @param names 属性列表
     */
    public static void remove(String... names) {
        Arrays.stream(names).forEach(get()::removeAttribute);
    }
}
