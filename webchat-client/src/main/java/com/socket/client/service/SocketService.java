package com.socket.client.service;

import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;

/**
 * @date 2022/1/11
 */
public interface SocketService {

    /**
     * 处理用户消息
     *
     * @return 回调数据
     */
    WsMsg parseUserMsg(WsMsg wsmsg, WsUser target);

    /**
     * 处理系统消息
     * 用户命令（无需权限执行）
     *
     * @return 回调数据
     */
    WsMsg parseSysMsg(WsMsg wsmsg, WsUser target);

    /**
     * 解析管理员命令
     *
     * @return 回调数据
     */
    WsMsg parseAdminSysMsg(WsMsg wsmsg, WsUser target);

    /**
     * 解析所有者命令
     *
     * @return 回调数据
     */
    WsMsg parseOwnerSysMsg(WsMsg wsmsg, WsUser target);
}
