package com.socket.client.feign.response;

import lombok.Data;

/**
 * 与{@link com.socket.core.model.enums.HttpStatus}保持一致
 */
@Data
public class FeignResponse<T> {
    /**
     * 状态码
     */
    private int code;
    /**
     * 本次请求是否成功
     */
    private boolean success;
    /**
     * 返回的消息
     */
    private String message;
    /**
     * 返回的数据
     */
    private T data;
    /**
     * 返回的时间戳
     */
    private long timestamp;
}
