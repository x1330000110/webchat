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

    public static void redirectIfNull(Object obj, HttpServletResponse response, String url) {
        if (obj == null) {
            redirect(response, url);
        }
    }


    public static void redirectIfTrue(boolean bool, HttpServletResponse response, String url) {
        if (bool) {
            redirect(response, url);
        }
    }

    @ExceptionHandler(RedirectException.class)
    private void isRedirectException() {
        // ignore
    }

    static class RedirectException extends RuntimeException {
    }
}
