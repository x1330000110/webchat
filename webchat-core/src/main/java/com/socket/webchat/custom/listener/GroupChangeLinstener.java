package com.socket.webchat.custom.listener;

import com.socket.webchat.custom.listener.event.GroupChangeEvent;
import org.springframework.context.ApplicationListener;

/**
 * 群组信息/成员变动监视器
 */
public interface GroupChangeLinstener extends ApplicationListener<GroupChangeEvent> {
    @Override
    default void onApplicationEvent(GroupChangeEvent event) {
        this.onGroupChange(event);
    }

    /**
     * 触发事件
     *
     * @param event 群组与用户信息
     */
    void onGroupChange(GroupChangeEvent event);
}
