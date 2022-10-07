package com.socket.webchat.custom;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import com.socket.webchat.constant.properties.FTPProperties;
import com.socket.webchat.model.FTPFile;
import com.socket.webchat.model.enums.FilePath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件上传管理服务
 *
 * @date 2022/8/1
 */
@Slf4j
@Component
public class FTPClient {
    private final FTPProperties config;

    FTPClient(FTPProperties config) {
        this.config = config;
    }

    /**
     * 每次连接必须创建一个新的会话
     *
     * @return {@linkplain Ftp}
     */
    private Ftp getClient() {
        Ftp ftp = new Ftp(config, FtpMode.Passive);
        ftp.cd("/");
        return ftp;
    }

    /**
     * 上传文件到FTP（自动生成文件名）
     *
     * @param path  保存目录
     * @param bytes 文件数据
     * @return 文件信息
     */
    public FTPFile upload(FilePath path, byte[] bytes) {
        // 生成文件名
        String hash = SecureUtil.hmacMd5(String.valueOf(bytes.length << bytes.length / 2)).digestHex(bytes);
        // 上传
        return upload(path, hash, new ByteArrayInputStream(bytes));
    }

    /**
     * 上传文件到FTP
     *
     * @param path   文件目录
     * @param name   文件名
     * @param stream 输入流
     * @return 文件信息
     */
    public FTPFile upload(FilePath path, String name, InputStream stream) {
        FTPFile file = new FTPFile(path, name);
        try (Ftp ftp = getClient()) {
            if (ftp.existFile(file.getPath()) || ftp.upload(path.getDirectory(), name, stream)) {
                return file;
            }
        } catch (IOException | IORuntimeException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    /**
     * 检查FTP文件是否存在
     *
     * @param path 文件类型
     * @param name 文件名
     * @return 是否存在
     */
    public boolean existFile(FilePath path, String name) {
        try (Ftp ftp = getClient()) {
            return ftp.existFile(path.getDirectory() + FTPFile.separator + name);
        } catch (IOException | IORuntimeException e) {
            log.warn(e.getMessage());
        }
        return false;
    }

    /**
     * 下载FTP文件
     *
     * @param path   目录
     * @param name   文件名
     * @param stream 输出流
     */
    public <T extends OutputStream> T download(FilePath path, String name, T stream) {
        String directory = path.getDirectory();
        try (Ftp ftp = getClient()) {
            if (!ftp.existFile(directory + FTPFile.separator + name)) {
                return null;
            }
            ftp.download(directory, name, stream);
        } catch (IOException | IORuntimeException e) {
            log.warn(e.getMessage());
        }
        return stream;
    }

    /**
     * 移除FTP文件
     *
     * @param maps key-散列文件 value-文件位置
     */
    public void deleteFiles(Map<String, String> maps) {
        try (Ftp ftp = getClient()) {
            List<String> collect = ftp.lsFiles(FilePath.BLOB.getDirectory(), null)
                    .stream()
                    .map(org.apache.commons.net.ftp.FTPFile::getName)
                    .filter(maps::containsKey)
                    .collect(Collectors.toList());
            maps.forEach((hash, path) -> {
                if (collect.contains(hash)) {
                    ftp.delFile(path);
                }
            });
        } catch (IOException | IORuntimeException e) {
            log.warn(e.getMessage());
        }
    }
}
