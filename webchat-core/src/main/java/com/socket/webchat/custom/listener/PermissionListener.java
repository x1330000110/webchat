package com.socket.webchat.custom.listener;

import org.springframework.context.ApplicationListener;

/**
 * 权限变动监视器
 */
public interface PermissionListener extends ApplicationListener<PermissionEvent> {
}
