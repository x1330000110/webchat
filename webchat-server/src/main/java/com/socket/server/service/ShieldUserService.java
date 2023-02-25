package com.socket.server.service;

import com.socket.core.model.po.ShieldUser;

public interface ShieldUserService extends BaseService<ShieldUser> {

    /**
     * 屏蔽/取消屏蔽 指定用户
     *
     * @param target 目标用户
     * @return 若成功屏蔽返回true, 取消屏蔽返回false
     */
    boolean shieldTarget(String target);
}
