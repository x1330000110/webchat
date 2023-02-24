package com.socket.webchat.custom.storage.ftp;

import cn.hutool.extra.ftp.FtpConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FTP对外开放的配置
 */
@Component
@ConfigurationProperties(prefix = "ftp.client")
public class FTPConfig extends FtpConfig {
}
