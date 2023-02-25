package com.socket.server.custom.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import com.socket.core.model.enums.FileType;
import com.socket.server.custom.storage.ResourceStorage;
import com.socket.server.properties.FTPProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * FTP文件资源映射与储存实现 <br>
 * （有关FTP映射文件到nginx请参阅 <a href="https://www.jianshu.com/p/e36e49c248e8">URL</a>）
 */
@Slf4j
@RequiredArgsConstructor
public class FTPResourceStorage implements ResourceStorage {
    private static final String ROOT = "/chatfile";
    private final FTPProperties config;

    @Override
    public String upload(FileType type, byte[] bytes, String hash) {
        String dist = ROOT + "/" + type.getKey();
        String path = dist + "/" + hash;
        try (Ftp session = getFtpSession()) {
            if (session.exist(path) || session.upload(dist, hash, new ByteArrayInputStream(bytes))) {
                return path;
            }
        } catch (IOException e) {
            log.warn("FTP文件上传错误：{}", e.getMessage());
        }
        throw new IllegalStateException("FTP文件上传失败：" + path);
    }

    @Override
    public byte[] download(String url) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Ftp session = getFtpSession()) {
            if (session.existFile(url)) {
                String name = FileUtil.getName(url);
                String dir = StrUtil.removeSuffix(url, name);
                session.download(dir, name, out);
            }
        } catch (IOException e) {
            log.debug("FTP下载文件错误：{}", e.getMessage());
        }
        return out.toByteArray();
    }

    @Override
    public String getOpenURL(String url) {
        return ROOT + "/" + url;
    }

    private Ftp getFtpSession() {
        return new Ftp(config, FtpMode.Passive);
    }
}
