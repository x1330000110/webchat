package com.socket.webchat.custom.listener;

import com.socket.webchat.custom.listener.event.UserChangeEvent;
import org.springframework.context.ApplicationListener;

/**
 * 用户资料变动监视器
 */
public interface UserChangeListener extends ApplicationListener<UserChangeEvent> {
    @Override
    default void onApplicationEvent(UserChangeEvent event) {
        this.onUserChange(event);
    }

    /**
     * 触发事件
     *
     * @param event 用户信息
     */
    void onUserChange(UserChangeEvent event);
}
