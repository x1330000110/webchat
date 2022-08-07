package com.socket.webchat.model.condition;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileCondition {
    private MultipartFile blob;
    private String mid;
    private String digest;
}
