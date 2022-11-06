package com.socket.webchat.exception;

import com.socket.webchat.model.enums.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.mail.MailSendException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnknownAccountException.class)
    public HttpStatus isUnknownAccountException() {
        return HttpStatus.FAILURE.message("账号不存在或已被注销");
    }

    @ExceptionHandler(IncorrectCredentialsException.class)
    public HttpStatus isIncorrectCredentialsException() {
        return HttpStatus.FAILURE.message("用户名密码不正确");
    }

    @ExceptionHandler({AccountException.class, UploadException.class, IllegalStateException.class})
    public HttpStatus isIllegalStateException(Exception e) {
        return HttpStatus.FAILURE.message(e.getMessage());
    }

    @ExceptionHandler(MailSendException.class)
    public HttpStatus isSendFailedException(Exception e) {
        return HttpStatus.FAILURE.message("邮箱验证码发送失败");
    }

    @ExceptionHandler(OffsiteLoginException.class)
    public HttpStatus isOffsiteLoginException(Exception e) {
        return HttpStatus.OFFSITE.message(e.getMessage());
    }

    @ExceptionHandler(AuthorizationException.class)
    public HttpStatus isAuthorizationException(Exception e) {
        return HttpStatus.FAILURE.message("权限不足");
    }

    @ExceptionHandler({NumberFormatException.class,
            StringIndexOutOfBoundsException.class,
            NullPointerException.class,
            ArrayIndexOutOfBoundsException.class,
            IllegalArgumentException.class
    })
    public HttpStatus isNumberFormatException(Exception e) {
        e.printStackTrace();
        return HttpStatus.FAILURE.message("参数不正确：{}", e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void isHttpRequestMethodNotSupportedException(HttpServletResponse response) throws IOException {
        response.sendRedirect("/error");
    }

    @ExceptionHandler(RedirectException.class)
    public void isRedirectException() {
        // Ignore
    }

    @ExceptionHandler(Exception.class)
    public HttpStatus isException(Exception e) {
        e.printStackTrace();
        return HttpStatus.UNKNOWN.message("服务器繁忙，请稍后再试");
    }
}
