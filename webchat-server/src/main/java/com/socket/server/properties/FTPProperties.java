package com.socket.server.properties;

import cn.hutool.extra.ftp.FtpConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * FTP对外开放的配置
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ftp.client")
public class FTPProperties extends FtpConfig {
}
