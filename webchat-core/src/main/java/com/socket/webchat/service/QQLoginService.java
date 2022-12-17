package com.socket.webchat.service;

import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.request.bean.QQAuth;

public interface QQLoginService {

    /**
     * 获取登录凭证信息
     *
     * @return {@link QQAuth}
     */
    QQAuth getLoginAuth();

    /**
     * 验证QQ登录状态
     *
     * @param qrsig {@link QQAuth#getQrsig()}
     * @return 扫码状态
     */
    HttpStatus state(String qrsig);
}
