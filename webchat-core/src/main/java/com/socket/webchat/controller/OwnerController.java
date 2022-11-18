package com.socket.webchat.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.Assert;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.custom.listener.command.PermissionEnum;
import com.socket.webchat.custom.listener.event.PermissionEvent;
import com.socket.webchat.custom.support.SettingSupport;
import com.socket.webchat.model.Announce;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.MessageCondition;
import com.socket.webchat.model.condition.SettingCondition;
import com.socket.webchat.model.condition.UserCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.Setting;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

/**
 * 所有者权限控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/owner")
public class OwnerController {
    private final ApplicationEventPublisher publisher;
    private final SettingSupport settingSupport;
    private final SysUserService sysUserService;
    private final RecordService recordService;
    private final RedisManager redisManager;

    @ModelAttribute
    public void checkPermission() {
        Assert.isTrue(Wss.getUser().isOwner(), "权限不足", InvalidRequestException::new);
    }

    @PostMapping("/alias")
    public void alias(@RequestBody UserCondition condition) {
        String uid = condition.getUid();
        String content = condition.getContent();
        sysUserService.updateAlias(uid, content);
        publisher.publishEvent(new PermissionEvent(publisher, uid, content, PermissionEnum.ALIAS));
    }

    @PostMapping("/role")
    public void role(@RequestBody UserCondition condition) {
        String uid = condition.getUid();
        UserRole role = sysUserService.switchRole(uid);
        Wss.getUser().setRole(role);
        publisher.publishEvent(new PermissionEvent(publisher, uid, role.getRole(), PermissionEnum.ROLE));
    }

    @PostMapping("/announce")
    public void announce(@RequestBody Announce announce) {
        String content = announce.getContent();
        redisManager.pushNotice(content);
        if (StrUtil.isNotEmpty(content)) {
            publisher.publishEvent(new PermissionEvent(publisher, content, PermissionEnum.ANNOUNCE));
        }
    }

    @Encrypted
    @PostMapping("/deleteUser")
    public HttpStatus deleteUser(@RequestBody UserCondition condition) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        String uid = condition.getUid();
        wrapper.eq(SysUser::getUid, uid);
        wrapper.set(SysUser::isDeleted, 1);
        boolean update = sysUserService.update(wrapper);
        if (update) {
            publisher.publishEvent(new PermissionEvent(publisher, uid, PermissionEnum.FOREVER));
        }
        return HttpStatus.of(update, "操作成功", "找不到此用户");
    }

    @Encrypted
    @PostMapping("/deleteMessage")
    public HttpStatus deleteMessage(@RequestBody MessageCondition condition) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::getMid, condition.getMid());
        // 所有者可直接移除消息
        boolean state = recordService.remove(wrapper);
        return HttpStatus.of(state, "操作成功", "找不到相关记录");
    }

    @PostMapping("/restartServer")
    public HttpStatus restartServer() {
        settingSupport.switchSetting(Setting.RESTART_SERVER);
        return HttpStatus.SUCCESS.body();
    }

    @PostMapping("/switchSetting")
    public HttpStatus switchSetting(@RequestBody SettingCondition condition) {
        settingSupport.switchSetting(condition.getSetting());
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/setting")
    public HttpStatus getSetting() {
        return HttpStatus.SUCCESS.body(settingSupport.getMap());
    }
}
