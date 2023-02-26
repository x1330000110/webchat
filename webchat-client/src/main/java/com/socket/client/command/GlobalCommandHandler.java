package com.socket.client.command;

import cn.hutool.json.JSONUtil;
import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.command.user.UserChangeHandler;
import com.socket.core.constant.Topics;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.model.command.topic.GroupChangeTopic;
import com.socket.core.model.command.topic.PermissionTopic;
import com.socket.core.model.command.topic.UserChangeTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 全局命令处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalCommandHandler {
    private final Map<String, PermissionHandler> permissionHandlers;
    private final Map<String, GroupChangeHandler> groupHandlers;
    private final Map<String, UserChangeHandler> userHandlers;

    /**
     * 群组事件
     */
    @KafkaListener(topics = Topics.GROUP_COMMAND, groupId = "GROUP")
    public void onGroupChange(ConsumerRecord<String, String> data) {
        String serial = data.value();
        log.info("收到MQ消息：{}", serial);
        GroupChangeTopic topic = JSONUtil.parseObj(serial).toBean(GroupChangeTopic.class);
        GroupEnum command = topic.getOperation();
        groupHandlers.get(command.getName()).invoke(topic);
    }

    /**
     * 用户事件
     */
    @KafkaListener(topics = Topics.USER_COMMAND, groupId = "USER")
    public void onUserChange(ConsumerRecord<String, String> data) {
        String serial = data.value();
        log.info("收到MQ消息：{}", serial);
        UserChangeTopic topic = JSONUtil.parseObj(serial).toBean(UserChangeTopic.class);
        UserEnum command = topic.getOperation();
        userHandlers.get(command.getName()).invoke(topic);
    }

    /**
     * 权限事件
     */
    @KafkaListener(topics = Topics.PERMISSION_COMMAND, groupId = "PERMISSION")
    public void onPermission(ConsumerRecord<String, String> data) {
        String serial = data.value();
        log.info("收到MQ消息：{}", serial);
        PermissionTopic topic = JSONUtil.parseObj(serial).toBean(PermissionTopic.class);
        PermissionEnum command = topic.getOperation();
        permissionHandlers.get(command.getName()).invoke(topic);
    }
}
