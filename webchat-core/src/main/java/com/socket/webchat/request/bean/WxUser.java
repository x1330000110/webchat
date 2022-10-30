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
    /**
     * 所在省份
     */
    private String province;
    /**
     * 所在城市
     */
    private String city;
    /**
     * 性别
     */
    private Integer sex;
}
