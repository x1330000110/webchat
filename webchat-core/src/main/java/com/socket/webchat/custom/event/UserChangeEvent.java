package com.socket.webchat.custom.event;

import com.socket.webchat.model.enums.UserEnum;
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
    private final String data;

    public UserChangeEvent(Object source, UserEnum operation, String data) {
        this(source, operation, data, Wss.getUserId());
    }

    public UserChangeEvent(Object source, UserEnum operation, String data, String uid) {
        super(source);
        this.operation = operation;
        this.data = data;
        this.uid = uid;
    }
}
