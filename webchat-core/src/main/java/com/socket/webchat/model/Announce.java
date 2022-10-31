package com.socket.webchat.model;

import lombok.Data;

/**
 * 公共信息
 */
@Data
public class Announce {
    /**
     * 公共内容
     */
    private String content;
    /**
     * 散列签名
     */
    private String digest;
    /**
     * 生成时间
     */
    private Long time;
}
