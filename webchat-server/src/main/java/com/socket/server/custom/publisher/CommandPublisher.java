package com.socket.server.custom.publisher;

import cn.hutool.json.JSONUtil;
import com.socket.core.constant.Topics;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.model.command.topic.GroupChangeTopic;
import com.socket.core.model.command.topic.PermissionTopic;
import com.socket.core.model.command.topic.UserChangeTopic;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import com.socket.server.util.ShiroUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 命令推送封装
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandPublisher {
    private final KafkaTemplate<String, String> messageQueue;

    public void pushGroupEvent(SysGroup group, GroupEnum command) {
        GroupChangeTopic topic = new GroupChangeTopic(group, command);
        String serial = JSONUtil.toJsonStr(topic);
        log.info("发送MQ消息：{}", serial);
        messageQueue.send(Topics.GROUP_COMMAND, serial);
    }

    public void pushGroupEvent(SysGroupUser user, GroupEnum command) {
        GroupChangeTopic topic = new GroupChangeTopic(user, command);
        String serial = JSONUtil.toJsonStr(topic);
        log.info("发送MQ消息：{}", serial);
        messageQueue.send(Topics.GROUP_COMMAND, serial);
    }

    public void pushPermissionEvent(String data, PermissionEnum command) {
        pushPermissionEvent(null, data, command);
    }

    public void pushPermissionEvent(String target, Object data, PermissionEnum command) {
        pushPermissionEvent(ShiroUser.getUserId(), target, data, command);
    }

    public void pushPermissionEvent(String self, String target, Object data, PermissionEnum command) {
        PermissionTopic topic = new PermissionTopic(self, target, data, command);
        String serial = JSONUtil.toJsonStr(topic);
        log.info("发送MQ消息：{}", serial);
        messageQueue.send(Topics.PERMISSION_COMMAND, serial);
    }

    public void pushUserEvent(String data, UserEnum command) {
        pushUserEvent(null, data, command);
    }

    public void pushUserEvent(String target, String data, UserEnum command) {
        UserChangeTopic topic = new UserChangeTopic(target, data, command);
        String serial = JSONUtil.toJsonStr(topic);
        log.info("发送MQ消息：{}", serial);
        messageQueue.send(Topics.USER_COMMAND, serial);
    }
}
