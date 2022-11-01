package com.socket.webchat.request.bean;

import lombok.Data;

/**
 * QQ用户信息
 */
@Data
public class QQUser {
    /**
     * 昵称
     */
    private String name;
    /**
     * 头像
     */
    private String img;
}