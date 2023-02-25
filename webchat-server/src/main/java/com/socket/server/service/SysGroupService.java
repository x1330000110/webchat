package com.socket.server.service;

import com.socket.core.model.po.SysGroup;
import com.socket.core.service.BaseService;

import java.util.List;

public interface SysGroupService extends BaseService<SysGroup> {
    /**
     * 创建群组
     *
     * @param groupName 群名
     * @param password  密码
     * @return 群组Id
     */
    String createGroup(String groupName, String password);

    /**
     * 移除群组内用户
     *
     * @param gid 群组ID
     * @param uid 用户ID
     * @return 是否成功
     */
    boolean removeUser(String gid, String uid);

    /**
     * 加入密码群组（若群组未设置密码直接加入成功）
     *
     * @param gid      群组id
     * @param uid      用户uid
     * @param password 密码
     * @return 群组成员uid
     */
    List<String> joinGroup(String gid, String uid, String password);

    /**
     * 加入群组
     *
     * @param gid 群组id
     * @param uid 用户uid
     * @return 群组成员uid
     */
    List<String> joinGroup(String gid, String uid);

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

    /**
     * 修改入群密码
     *
     * @param gid      群组id
     * @param password 新密码
     * @return
     */
    boolean updatePassword(String gid, String password);
}
