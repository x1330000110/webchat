package com.socket.webchat.util;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ControllerAdvice
public class RedirectUtil {
    private static final RedirectException EXCEPTION = new RedirectException();

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
        throw EXCEPTION;
    }

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

    @ExceptionHandler(RedirectException.class)
    private void isRedirectException() {
        // Ignore
    }

    static class RedirectException extends RuntimeException {
        // Inner exception class
    }
}
