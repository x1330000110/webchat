package com.socket.webchat.custom.ftp;

import com.socket.webchat.model.enums.FilePath;
import com.socket.webchat.service.UploadService;
import lombok.Data;

/**
 * FTP基础文件信息
 *
 * @date 2022/8/2
 */
@Data
public class FTPFile {
    public static final char separator = '/';
    private String parent;
    private String name;

    public FTPFile(FilePath path, String name) {
        this.parent = path.getName();
        this.name = name;
    }

    /**
     * 获取FTP映射路径
     *
     * @return 映射路径
     */
    public String getMapping() {
        return UploadService.MAPPING + parent + separator + name;
    }
}
