package com.socket.webchat.service;

import cn.hutool.core.util.StrUtil;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.condition.URLCondition;
import com.socket.webchat.model.enums.FileType;

import java.io.IOException;

public interface UploadService extends BaseService<ChatRecordFile> {

    /**
     * 获取散列文件实际映射路径
     *
     * @param type 文件类型
     * @param hash 散列名
     * @return URL
     */
    default String getMapping(FileType type, String hash) {
        return StrUtil.format("/resource/{}/{}", type.getKey(), hash);
    }

    /**
     * 保存文件到FTP服务器
     *
     * @param condition 文件数据
     * @param type      文件类型
     * @return 映射位置
     */
    String upload(FileCondition condition, FileType type) throws IOException;

    /**
     * 转换语音消息到文字
     *
     * @param mid 消息id
     * @return 文字
     */
    String convertText(String mid);

    /**
     * 通过MID获取映射的文件
     *
     * @param mid 消息id
     * @return lanzou url
     */
    String getResourceURL(String mid);

    /**
     * 通过保存的散列文件名与类型获取映射的文件
     *
     * @param type 文件类型
     * @param hash 散列名
     * @return lanzou url
     */
    String getResourceURL(FileType type, String hash);

    /**
     * 保存要解析的URL地址
     *
     * @param condition 条件
     */
    void saveResolve(URLCondition condition);
}
