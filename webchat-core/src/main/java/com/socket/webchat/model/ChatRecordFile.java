package com.socket.webchat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 消息文件映射表
 *
 * @date 2022/6/10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatRecordFile extends BaseModel {
    /**
     * 消息id
     */
    private String mid;
    /**
     * 文件大小
     */
    private long size;
    /**
     * 散列名称
     */
    private String hash;

    public ChatRecordFile(String mid, String hash, long size) {
        this.mid = mid;
        this.hash = hash;
        this.size = size;
    }
}
