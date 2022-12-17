package com.socket.webchat.request.bean;

import lombok.Data;

/**
 * QQ登录信息
 */
@Data
public class QQAuth {
    /**
     * Base64登录二维码
     */
    private String qrCode;
    /**
     * 登录签名
     */
    private String qrsig;
}
