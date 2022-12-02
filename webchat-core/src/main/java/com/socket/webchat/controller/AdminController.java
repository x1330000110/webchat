package com.socket.webchat.controller;

import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.Assert;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.model.command.impl.PermissEnum;
import com.socket.webchat.model.condition.LimitCondition;
import com.socket.webchat.util.Publisher;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final RedisManager redisManager;
    private final Publisher publisher;

    @ModelAttribute
    public void checkPermission() {
        Assert.isTrue(Wss.getUser().isAdmin(), "权限不足", InvalidRequestException::new);
    }

    @PostMapping("/mute")
    public void mute(@RequestBody LimitCondition condition) {
        String guid = condition.getGuid();
        Long time = condition.getTime();
        redisManager.setMute(guid, time);
        publisher.pushPermissionEvent(guid, time, PermissEnum.MUTE);
    }

    @PostMapping("/lock")
    public void lock(@RequestBody LimitCondition condition) {
        String guid = condition.getGuid();
        Long time = condition.getTime();
        redisManager.setLock(guid, time);
        publisher.pushPermissionEvent(guid, time, PermissEnum.LOCK);
    }
}
