package com.socket.webchat.service;

import com.socket.webchat.model.ChatRecord;

import java.util.Collection;
import java.util.List;

public interface RecordService extends BaseService<ChatRecord> {
    /**
     * 获取最近指定条聊天信息
     *
     * @param mid    最久的消息id
     * @param target 目标用户
     * @return 聊天记录
     */
    List<ChatRecord> getRecords(String mid, String target);

    /**
     * 撤回消息（未送达必定撤回成功）
     *
     * @param mid 消息id
     * @return 是否成功
     */
    boolean withdrawMessage(String mid);

    /**
     * 移除消息（仅自己）
     *
     * @param mid 消息id
     * @return 是否成功
     */
    boolean removeMessage(String mid);

    /**
     * 删除当前用户对于目标用户的所有消息（更新消息标记offset）
     *
     * @param target 目标uid
     */
    void removeAllMessage(String target);

    /**
     * 同步指定消息为已读
     *
     * @param mid    发信人uid/消息mid
     * @param target 变更已读的uid
     */
    void readMessage(String mid, String target);

    /**
     * 同步指定用户所有消息为已读
     *
     * @param uid    发起人
     * @param target 变更已读的uid
     * @param audio  是否包括语音消息
     */
    void readAllMessage(String uid, String target, boolean audio);

    /**
     * 获取发送到此用户的所有人的最新消息，返回的ChatRecord#uid都不相同
     *
     * @param uid 用户uid
     * @return 关联未读消息表
     */
    Collection<ChatRecord> getLatestUnreadMessages(String uid);
}
