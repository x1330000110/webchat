package com.socket.webchat.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpRequest;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.webchat.custom.listener.PermissionEvent;
import com.socket.webchat.custom.listener.PermissionOperation;
import com.socket.webchat.model.Announce;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.UserCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.service.ShieldUserService;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final ApplicationEventPublisher publisher;
    private final ShieldUserService shieldUserService;
    private final SysUserService sysUserService;
    private final RedisClient<String> redis;

    @Encrypted
    @PostMapping(value = "/material")
    public HttpStatus material(@RequestBody SysUser sysUser) {
        sysUserService.updateMaterial(sysUser);
        return HttpStatus.SUCCESS.message("修改成功");
    }

    @Encrypted
    @PostMapping("/avatar")
    public HttpStatus updateAvatar(MultipartFile blob) throws IOException {
        String mapping = sysUserService.updateAvatar(blob.getBytes());
        return HttpStatus.SUCCESS.body("修改成功", mapping);
    }

    @Encrypted
    @PostMapping("/email")
    public HttpStatus updateEmail(@RequestBody EmailCondition condition) {
        sysUserService.updateEmail(condition);
        return HttpStatus.SUCCESS.message("修改成功");
    }

    @PostMapping("/logout")
    public HttpStatus logout() {
        SecurityUtils.getSubject().logout();
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/{uid}")
    public HttpStatus userInfo(@PathVariable String uid) {
        SysUser user = sysUserService.getUserInfo(uid);
        return HttpStatus.SUCCESS.body(user);
    }

    @GetMapping("/notice")
    public HttpStatus getNotice(String digest) {
        RedisMap<String, ?> map = redis.withMap(RedisTree.ANNOUNCE.get());
        Announce announce = BeanUtil.toBean(map, Announce.class);
        // 散列id不同 表示发布新内容
        if (announce != null && !Objects.equals(digest, announce.getDigest())) {
            return HttpStatus.SUCCESS.body(announce);
        }
        return HttpStatus.FAILURE.body();
    }

    @GetMapping("/sentence")
    public String sentence() {
        return HttpRequest.get("https://api.leafone.cn/api/yiyan").execute().body();
    }

    @PostMapping("/shield")
    public HttpStatus shield(@RequestBody UserCondition condition) {
        String uid = condition.getUid();
        boolean b = shieldUserService.shieldTarget(uid);
        publisher.publishEvent(new PermissionEvent(publisher, uid, null, PermissionOperation.SHIELD));
        return HttpStatus.of(b, "屏蔽成功", "取消屏蔽");
    }
}
