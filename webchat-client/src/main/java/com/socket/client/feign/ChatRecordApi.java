package com.socket.client.feign;

import com.socket.client.feign.response.FeignResponse;
import com.socket.core.model.condition.MessageCondition;
import com.socket.core.model.po.ChatRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@FeignClient("webchat-server")
@RequestMapping("/message")
public interface ChatRecordApi {
    /**
     * 同步指定用户所有消息为已读
     */
    @PostMapping(value = "/reading")
    void readAllMessage(MessageCondition condition);

    /**
     * 获取发送到此用户的所有人的最新消息
     */
    @GetMapping(value = "/latest")
    FeignResponse<Map<String, ChatRecord>> getLatest();
}
