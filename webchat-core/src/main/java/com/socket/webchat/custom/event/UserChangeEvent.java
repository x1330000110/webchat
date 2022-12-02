package com.socket.webchat.custom.event;

import com.socket.webchat.model.command.impl.UserEnum;
import com.socket.webchat.util.Wss;
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
    private final String uid;
    /**
     * 新数据
     */
    private final String param;

    public UserChangeEvent(Object source, UserEnum operation, String param) {
        this(source, operation, param, Wss.getUserId());
    }

    public UserChangeEvent(Object source, UserEnum operation, String param, String uid) {
        super(source);
        this.operation = operation;
        this.param = param;
        this.uid = uid;
    }
}
