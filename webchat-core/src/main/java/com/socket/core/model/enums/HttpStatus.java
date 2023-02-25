package com.socket.core.model.enums;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

/**
 * 统一HTTP状态枚举<br>
 * 注：{@link #message(String, Object...)}和{@link #body(Object...)}方法不能一起使用，
 * 请使用提供的{@link #body(String, Object) }方法。<br>
 * 当使用{@link #SUCCESS}枚举时返回{@link #success}参数为true，其他枚举{@link #success}参数为false<br>
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
public enum HttpStatus {
    /**
     * 操作已接收但等待
     */
    WAITTING(100),
    /**
     * 操作成功完成
     */
    SUCCESS(200),
    /**
     * 操作未完成
     */
    FAILURE(400),
    /**
     * 安全验证失败
     */
    SECURITY(4001),
    /**
     * 请求已过期
     */
    EXPIRED(4002),
    /**
     * 异地登录验证
     */
    OFFSITE(4003),
    /**
     * 继续执行当前请求要求认证身份
     */
    UNAUTHORIZED(403),
    /**
     * 请求资源已存在
     */
    EXISTED(4005),
    /**
     * 服务器错误
     */
    UNKNOWN(500);

    /**
     * 状态码
     */
    private final int code;
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
    private Object data;
    /**
     * 返回的时间戳
     */
    private long timestamp;

    HttpStatus(int code) {
        this.code = code;
    }

    /**
     * 快速构建{@link HttpStatus}，并设置本次执行成功/失败关键词
     *
     * @param state   状态
     * @param message 关键词
     * @return {@link HttpStatus}
     */
    public static HttpStatus state(boolean state, String message) {
        return of(state, message.concat("成功"), message.concat("失败"));
    }

    /**
     * 快速构建{@link HttpStatus}，并设置成功/失败回调消息
     *
     * @param state 状态
     * @param s     成功消息
     * @param f     失败消息
     * @return {@link HttpStatus}
     */
    public static HttpStatus of(boolean state, String s, String f) {
        return state ? SUCCESS.message(s) : FAILURE.message(f);
    }

    /**
     * 设置返回给客户端的消息 <br>
     * <b>注：此方法会清除{@link #data}参数</b>
     *
     * @param message 消息 (支持占位符)
     * @return {@link HttpStatus}
     */
    public HttpStatus message(String message, Object... args) {
        this.success = this == SUCCESS;
        this.data = null;
        this.message = StrUtil.format(message, args);
        return this;
    }

    /**
     * 映射执行时间
     */
    public long getTimestamp() {
        return timestamp = System.currentTimeMillis();
    }

    /**
     * 设置返回给客户端的数据 <br>
     * <b>注：返回的 {@link #message}为枚举名称</b>
     *
     * @param data 数据
     * @return {@link HttpStatus}
     */
    public HttpStatus body(Object... data) {
        this.success = this == SUCCESS;
        this.message = this.name();
        this.data = data.length == 0 ? null : data.length == 1 ? data[0] : data;
        return this;
    }

    /**
     * 设置返回给客户端的消息和数据
     *
     * @param message 消息
     * @param data    数据
     * @return {@link HttpStatus}
     */
    public HttpStatus body(String message, Object data) {
        this.success = this == SUCCESS;
        this.message = message;
        this.data = data;
        return this;
    }
}
