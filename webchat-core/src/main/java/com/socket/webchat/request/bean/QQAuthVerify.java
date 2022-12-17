package com.socket.webchat.request.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QQ授权信息
 */
@Data
@NoArgsConstructor
public class QQAuthVerify {
    /**
     * 登录状态
     */
    private String state;
    /**
     * QQ uin
     */
    private String uin;
    /**
     * QQ skey
     */
    private String skey;

    public QQAuthVerify(String state) {
        this.state = state;
    }
}
