package com.socket.webchat.custom.storage.ftp;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import com.socket.webchat.custom.storage.ResourceStorage;
import com.socket.webchat.model.enums.FileType;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * FTP文件资源映射与储存实现 <br>
 * （有关FTP映射文件到nginx请参阅 <a href="https://www.jianshu.com/p/e36e49c248e8">URL</a>）
 */
@RequiredArgsConstructor
public class FTPResourceStorage implements ResourceStorage {
    private static final String ROOT = "/chatfile";
    private final FTPConfig config;

    @Override
    public String upload(FileType type, byte[] bytes, String hash) {
        Ftp session = getFtpSession();
        String path = ROOT + "/" + type.getKey();
        session.upload(path, hash, new ByteArrayInputStream(bytes));
        return path + "/" + hash;
    }

    @Override
    public byte[] download(String url) {
        Ftp session = getFtpSession();
        final String name = FileUtil.getName(url);
        final String dir = StrUtil.removeSuffix(url, name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.download(dir, name, out);
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
