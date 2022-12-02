package com.socket.webchat.service;

import com.socket.webchat.model.SysGroup;

public interface SysGroupService extends BaseService<SysGroup> {
    /**
     * 创建群组
     *
     * @param groupName 群名
     * @return 群组Id
     */
    String createGroup(String groupName);

    /**
     * 移除群组内用户
     *
     * @param stater 发起者
     * @param gid    群组ID
     * @param uid    用户ID
     * @return 是否成功
     */
    boolean removeUser(String stater, String gid, String uid);

    /**
     * 加入群组
     *
     * @param gid 群组id
     * @param uid 用户uid
     * @return 是否成功
     */
    boolean joinGroup(String gid, String uid);

    /**
     * 解散群组
     *
     * @param gid 群组id
     * @return 是否成功
     */
    boolean dissolveGroup(String gid);

    /**
     * 退出群组
     *
     * @param gid 群组id
     * @return 是否成功
     */
    boolean exitGroup(String gid);
}
