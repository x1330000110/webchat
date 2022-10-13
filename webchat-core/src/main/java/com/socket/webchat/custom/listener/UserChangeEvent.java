package com.socket.webchat.custom.listener;

import com.socket.webchat.model.enums.UserOperation;
import com.socket.webchat.util.Wss;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserChangeEvent extends ApplicationEvent {
    /**
     * 更新值
     */
    private final UserOperation operation;
    /**
     * 新数据
     */
    private final String data;
    /**
     * 触发者
     */
    private final String uid;

    public UserChangeEvent(Object source, String data, UserOperation operation) {
        super(source);
        this.data = data;
        this.operation = operation;
        this.uid = Wss.getUserId();
    }
}
