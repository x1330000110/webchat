package com.socket.core.model.command.topic;

import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.util.ShiroUser;
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

    public PermissionTopic(Object param, PermissionEnum operation) {
        this(null, param, operation);
    }

    public PermissionTopic(String target, Object param, PermissionEnum operation) {
        this(ShiroUser.getUserId(), target, param, operation);
    }

    public PermissionTopic(String self, String target, Object param, PermissionEnum operation) {
        this.self = self;
        this.target = target;
        this.param = param;
        this.operation = operation;
    }
}
