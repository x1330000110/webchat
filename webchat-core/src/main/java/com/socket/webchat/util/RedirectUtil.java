package com.socket.webchat.util;

import com.socket.webchat.exception.RedirectException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectUtil {

    /**
     * <code>TRUE</code>判定的转发操作
     *
     * @param bool     转发条件
     * @param response {@linkplain HttpServletResponse}
     * @param url      跳转地址
     * @see #redirect(HttpServletResponse, String)
     */
    public static void redirectIf(boolean bool, HttpServletResponse response, String url) {
        if (bool) {
            redirect(response, url);
        }
    }

    /**
     * 转发快捷操作，调用此方法将不会向下继续执行代码且不返回任何结果
     *
     * @param response {@linkplain HttpServletResponse}
     * @param url      跳转地址
     */
    public static void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException ignored) {
        }
        throw new RedirectException();
    }

    /**
     * <code>NULL</code>判定的转发操作
     *
     * @param obj      为空时转发
     * @param response {@linkplain HttpServletResponse}
     * @param url      跳转地址
     * @see #redirect(HttpServletResponse, String)
     */
    public static void redirectIfNull(Object obj, HttpServletResponse response, String url) {
        if (obj == null) {
            redirect(response, url);
        }
    }
}
