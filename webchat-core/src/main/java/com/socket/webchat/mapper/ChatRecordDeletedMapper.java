package com.socket.webchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socket.webchat.model.ChatRecordDeleted;
import org.springframework.stereotype.Repository;

/**
 * 单条聊天记录删除管理
 *
 * @date 2022/8/16
 */
@Repository
public interface ChatRecordDeletedMapper extends BaseMapper<ChatRecordDeleted> {
}
