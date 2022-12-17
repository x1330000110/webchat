package com.socket.webchat.request.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信用户信息
 *
 * @date 2021/7/9
 */
@Data
public class WxUser implements Serializable {
    /**
     * 用户ID
     */
    private String openid;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像地址
     */
    private String headimgurl;
}
