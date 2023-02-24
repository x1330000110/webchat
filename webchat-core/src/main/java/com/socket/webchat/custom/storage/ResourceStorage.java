package com.socket.webchat.custom.storage;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.socket.webchat.model.enums.FileType;

/**
 * 资源储存适配接口
 */
public interface ResourceStorage {
    String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36 Edg/106.0.1370.37";

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
     * 获取指定资源文件数据
     *
     * @param url 资源地址
     * @return 文件数据
     */
    default byte[] download(String url) {
        return HttpRequest.get(getOriginalURL(url))
                .header(Header.USER_AGENT, USER_AGENT).
                execute()
                .bodyBytes();
    }

    /**
     * 获取指定URL的原始路径（如果有必要）
     *
     * @param url 资源地址
     * @return 原始路径
     */
    default String getOriginalURL(String url) {
        return url;
    }
}
