package com.socket.webchat.constant.properties;

import cn.hutool.extra.ftp.FtpConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FTP配置公开类
 *
 * @date 2022/8/1
 */
@Component
@ConfigurationProperties("webchat.ftp")
public class FTPProperties extends FtpConfig {
}