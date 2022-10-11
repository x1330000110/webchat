package com.socket.webchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socket.webchat.model.ChatRecord;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface RecordService extends IService<ChatRecord> {
    /**
     * 获取最近指定条聊天信息
     *
     * @param mid    最久的消息id
     * @param target 目标用户
     * @return 聊天记录
     */
    List<ChatRecord> getRecords(String mid, String target);

    /**
     * 通过消息id删除消息
     *
     * @param uid 发起者
     * @param mid 消息id
     * @return 成功删除，返回被删除的消息
     */
    ChatRecord removeMessage(String uid, String mid);

    /**
     * 删除指定用户所有消息（更新消息标记offset）
     *
     * @param uid    发起者
     * @param target 目标uid
     */
    void removeAllMessage(String uid, String target);

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
     * 获取指定用户所有未读的消息
     *
     * @param uid 用户uid
     * @return 关联未读消息表
     */
    Map<String, SortedSet<ChatRecord>> getUnreadMessages(String uid);

    /**
     * 移除消息（只能移除消息目标是自己或自己发送的消息）
     *
     * @param mid 消息id
     * @return 是否成功
     */
    boolean removeMessageWithSelf(String mid);
}
