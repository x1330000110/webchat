package com.socket.webchat.custom.storage;

import com.socket.webchat.model.enums.FileType;

/**
 * 资源储存适配接口
 */
public interface ResourceStorage {
    /**
     * 上传指定资源文件
     *
     * @param type  文件类型
     * @param bytes 文件数据
     * @param hash  一致性签名
     * @return 资源地址
     */
    String upload(FileType type, byte[] bytes, String hash);

    /**
     * 下载指定资源文件
     *
     * @param url 资源地址
     * @return 文件数据
     */
    byte[] download(String url);

    /**
     * 获取指定URL的外部访问路径
     *
     * @param url 资源地址
     * @return 原始路径
     */
    String getOpenURL(String url);
}
