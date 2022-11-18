package com.socket.webchat.custom.listener;

import com.socket.webchat.custom.listener.event.PermissionEvent;
import org.springframework.context.ApplicationListener;

/**
 * 权限变动监视器
 */
public interface PermissionListener extends ApplicationListener<PermissionEvent> {
    @Override
    default void onApplicationEvent(PermissionEvent event) {
        this.onPermission(event);
    }

    /**
     * 触发事件
     *
     * @param event 数据
     */
    void onPermission(PermissionEvent event);
}
