package com.socket.webchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socket.webchat.model.SysGroup;

public interface SysGroupService extends IService<SysGroup> {
    /**
     * 创建群组
     *
     * @param groupName 群名
     */
    void createGroup(String groupName);

    /**
     * 加入用户
     *
     * @param groupId 群组id
     * @param uid     用户id
     * @return 是否成功
     */
    boolean joinGroup(String groupId, String uid);

    /**
     * 移除群组内用户
     *
     * @param stater  发起者
     * @param groupId 群组ID
     * @param uid     用户ID
     * @return 移除结果
     */
    boolean removeUser(String stater, String groupId, String uid);
}
