package com.socket.core.model.command.topic;

import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.util.ShiroUser;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户状态变更消息
 */
@Data
@NoArgsConstructor
public class UserChangeTopic {
    /**
     * 触发事件
     */
    private UserEnum operation;
    /**
     * 触发者
     */
    private String target;
    /**
     * 新数据
     */
    private String param;

    public UserChangeTopic(String param, UserEnum operation) {
        this(ShiroUser.getUserId(), param, operation);
    }

    public UserChangeTopic(String target, String param, UserEnum operation) {
        this.operation = operation;
        this.param = param;
        this.target = target;
    }
}
