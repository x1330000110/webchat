package com.socket.webchat.custom.event;

import com.socket.webchat.model.command.impl.UserEnum;
import com.socket.webchat.util.ShiroUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserChangeEvent extends ApplicationEvent {
    /**
     * 触发事件
     */
    private final UserEnum operation;
    /**
     * 触发者
     */
    private final String target;
    /**
     * 新数据
     */
    private final String param;

    public UserChangeEvent(Object source, String param, UserEnum operation) {
        this(source, ShiroUser.getUserId(), param, operation);
    }

    public UserChangeEvent(Object source, String target, String param, UserEnum operation) {
        super(source);
        this.operation = operation;
        this.param = param;
        this.target = target;
    }
}
