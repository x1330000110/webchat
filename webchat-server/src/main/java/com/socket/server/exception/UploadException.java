package com.socket.server.exception;

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
