package com.socket.webchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socket.webchat.model.SysGroup;

public interface SysGroupService extends IService<SysGroup> {
    /**
     * 创建群组
     *
     * @param groupName 群名
     * @return 是否成功
     */
    boolean createGroup(String groupName);

    /**
     * 移除群组内用户
     *
     * @param stater  发起者
     * @param groupId 群组ID
     * @param uid     用户ID
     * @return 是否成功
     */
    boolean removeUser(String stater, String groupId, String uid);

    /**
     * 加入用户
     *
     * @param groupId 群组id
     * @param uid     用户id
     * @return 是否成功
     */
    boolean joinGroup(String groupId, String uid);

    /**
     * 移除群组
     *
     * @param owner   所有者id
     * @param groupId 群组id
     * @return 是否成功
     */
    boolean removeGroup(String owner, String groupId);
}
