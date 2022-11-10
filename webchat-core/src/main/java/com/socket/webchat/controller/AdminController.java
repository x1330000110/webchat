package com.socket.webchat.controller;

import com.socket.secure.exception.InvalidRequestException;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.custom.listener.PermissionEvent;
import com.socket.webchat.custom.listener.PermissionOperation;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final ApplicationEventPublisher publisher;
    private final RedisManager redisManager;

    @ModelAttribute
    public void checkPermission() {
        Assert.isTrue(Wss.getUser().isAdmin(), "权限不足", InvalidRequestException::new);
    }

    @PostMapping("/mute")
    public void mute(String target, Long time) {
        redisManager.setMute(target, time);
        publisher.publishEvent(new PermissionEvent(publisher, target, time.toString(), PermissionOperation.MUTE));
    }

    @PostMapping("/lock")
    public void lock(String target, Long time) {
        redisManager.setLock(target, time);
        publisher.publishEvent(new PermissionEvent(publisher, target, time.toString(), PermissionOperation.LOCK));
    }
}
