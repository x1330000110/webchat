package com.socket.core.model.po;

import com.socket.core.model.base.BaseModel;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatRecordFile extends BaseModel {
    /**
     * 消息id
     */
    private String mid;
    /**
     * 文件类型
     */
    private String type;
    /**
     * 文件路径
     */
    private String url;
    /**
     * 散列名称
     */
    private String hash;
    /**
     * 文件大小
     */
    private Long size;
}
