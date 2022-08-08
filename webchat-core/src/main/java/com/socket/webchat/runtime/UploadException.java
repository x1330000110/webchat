package com.socket.webchat.runtime;

/**
 * 文件上传失败异常
 */
public class UploadException extends IllegalStateException {
    public UploadException() {
    }

    public UploadException(String s) {
        super(s);
    }
}
