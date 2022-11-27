package com.socket.webchat.controller;

import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.Assert;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.model.condition.LimitCondition;
import com.socket.webchat.model.enums.PermissionEnum;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

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
    public void mute(@RequestBody LimitCondition condition) {
        String uid = condition.getUid();
        Long time = condition.getTime();
        redisManager.setMute(uid, time);
        publisher.publishEvent(new PermissionEvent(publisher, uid, time.toString(), PermissionEnum.MUTE));
    }

    @PostMapping("/lock")
    public void lock(@RequestBody LimitCondition condition) {
        String uid = condition.getUid();
        Long time = condition.getTime();
        redisManager.setLock(uid, time);
        publisher.publishEvent(new PermissionEvent(publisher, uid, time.toString(), PermissionEnum.LOCK));
    }
}
