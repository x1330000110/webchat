package com.socket.webchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socket.webchat.model.ChatRecordOffset;
import org.springframework.stereotype.Repository;

/**
 * 聊天记录删除标记管理
 *
 * @date 2022/4/4
 */
@Repository
public interface ChatRecordOffsetMapper extends BaseMapper<ChatRecordOffset> {
}
