package com.socket.core.model;

import cn.hutool.crypto.digest.MD5;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公告信息
 */
@Data
@NoArgsConstructor
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

    public Announce(String content) {
        this.content = content;
        this.digest = MD5.create().digestHex(content);
        this.time = System.currentTimeMillis();
    }
}
