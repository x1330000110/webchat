package com.socket.server.service;

import com.socket.core.model.enums.HttpStatus;
import com.socket.server.request.vo.QQAuthReq;

public interface QQLoginService {

    /**
     * 获取登录凭证信息
     *
     * @return {@link QQAuthReq}
     */
    QQAuthReq getLoginAuth();

    /**
     * 验证QQ登录状态
     *
     * @param qrsig {@link QQAuthReq#getQrsig()}
     * @return 扫码状态
     */
    HttpStatus state(String qrsig);
}
