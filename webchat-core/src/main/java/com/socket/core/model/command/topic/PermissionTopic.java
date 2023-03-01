package com.socket.core.model.command.topic;

import com.socket.core.model.command.impl.PermissionEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限命令执行消息
 */
@Data
@NoArgsConstructor
public class PermissionTopic {
    private PermissionEnum operation;
    private String self;
    private String target;
    private Object param;

    public PermissionTopic(String self, String target, Object param, PermissionEnum operation) {
        this.self = self;
        this.target = target;
        this.param = param;
        this.operation = operation;
    }
}
