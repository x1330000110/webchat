package com.socket.webchat.service;

import com.socket.webchat.model.SysGroup;

public interface SysGroupService extends BaseService<SysGroup> {
    /**
     * 创建群组
     *
     * @param groupName 群名
     * @param img       群头像
     * @return 群组Id
     */
    String createGroup(String groupName, String img);

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
     * 加入群组
     *
     * @param groupId 群组id
     * @param uid     用户uid
     * @return 是否成功
     */
    boolean joinGroup(String groupId, String uid);

    /**
     * 解散群组
     *
     * @param groupId 群组id
     * @return 是否成功
     */
    boolean dissolveGroup(String groupId);

    /**
     * 退出群组
     *
     * @param groupId 群组id
     * @return 是否成功
     */
    boolean exitGroup(String groupId);
}
