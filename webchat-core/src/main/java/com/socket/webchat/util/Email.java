package com.socket.webchat.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.socket.webchat.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Component
@RequiredArgsConstructor
public class Email {
    private final JavaMailSenderImpl mailSender;
    private final MailProperties properties;
    private final String content = IoUtil.readUtf8(getClass().getClassLoader().getResourceAsStream("static/mail.html"));

    /**
     * 向指定mail发送验证码
     *
     * @param mail 收件人
     * @return 验证码
     */
    @SneakyThrows
    public String send(String mail) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        String code = RandomUtil.randomNumbers(6);
        helper.setSubject("验证码");
        helper.setFrom(StrUtil.format("WebChat认证邮件<{}>", properties.getUsername()));
        final int time = Constants.EMAIL_CODE_VALID_TIME;
        helper.setText(StrUtil.format(content, code, time, code, time), true);
        helper.setTo(mail);
        mailSender.send(message);
        return code;
    }
}
