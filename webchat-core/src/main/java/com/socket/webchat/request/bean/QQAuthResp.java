package com.socket.webchat.request.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QQ授权信息
 */
@Data
@NoArgsConstructor
public class QQAuthResp {
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

    public QQAuthResp(String state) {
        this.state = state;
    }
}
