package com.socket.webchat.controller;

import lombok.Data;

/**
 * 视频解析参数
 */
@Data
public class URLCondition {
    /**
     * 视频地址
     */
    private String url;
    /**
     * 消息mid
     */
    private String mid;
    /**
     * 视频类型
     */
    private String type;
}
