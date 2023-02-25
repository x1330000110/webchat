package com.socket.core.model.condition;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileCondition {
    /**
     * 文件
     */
    private MultipartFile blob;
    /**
     * 绑定的消息mid
     */
    private String mid;
}
