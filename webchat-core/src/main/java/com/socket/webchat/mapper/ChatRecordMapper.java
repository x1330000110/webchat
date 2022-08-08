package com.socket.webchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socket.webchat.model.ChatRecord;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {
}
