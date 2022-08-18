package com.socket.webchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FilePath;

import java.io.IOException;
import java.io.OutputStream;

public interface UploadService extends IService<ChatRecordFile> {
    /**
     * 控制器 {@linkplain com.socket.webchat.controller.UploadController UploadController} 公开的URI路径
     */
    String MAPPING = "/resource";

    /**
     * 保存文件到FTP服务器
     *
     * @param condition 文件数据
     * @param path      文件类型
     * @return 映射位置
     */
    String upload(FileCondition condition, FilePath path) throws IOException;

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
     * @param mid    消息id
     * @param stream 输出流
     * @return stream
     */
    <T extends OutputStream> T writeStream(String mid, T stream);

    /**
     * 通过保存的散列文件名与类型获取映射的文件
     *
     * @param path   文件类型
     * @param hash   散列名
     * @param stream 输出流
     * @return stream
     */
    <T extends OutputStream> T writeStream(FilePath path, String hash, T stream);
}
