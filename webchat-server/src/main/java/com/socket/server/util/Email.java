package com.socket.server.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.socket.core.constant.Constants;
import com.socket.secure.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class Email implements InitializingBean {
    private final JavaMailSenderImpl mailSender;
    private final MailProperties properties;
    private String content;

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

    @Override
    public void afterPropertiesSet() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("static/mail.html");
        Assert.notNull(stream, "找不到邮件模板文件，请检查", BeanCreationException::new);
        this.content = IoUtil.readUtf8(stream);
    }
}
