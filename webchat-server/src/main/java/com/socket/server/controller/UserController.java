package com.socket.server.controller;

import cn.hutool.core.bean.BeanUtil;
import com.socket.core.custom.publisher.CommandPublisher;
import com.socket.core.model.Announce;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.condition.EmailCondition;
import com.socket.core.model.condition.UserCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.po.SysUser;
import com.socket.core.util.RedisClient;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.server.service.ShieldUserService;
import com.socket.server.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final ShieldUserService shieldUserService;
    private final SysUserService sysUserService;
    private final RedisClient<String> redis;
    private final CommandPublisher publisher;

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

    @GetMapping("/{guid}")
    public HttpStatus userInfo(@PathVariable String guid) {
        SysUser user = sysUserService.getUserInfo(guid);
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

    @PostMapping("/shield")
    public HttpStatus shield(@RequestBody UserCondition condition) {
        String guid = condition.getGuid();
        boolean b = shieldUserService.shieldTarget(guid);
        publisher.pushPermissionEvent(guid, null, PermissionEnum.SHIELD);
        return HttpStatus.of(b, "屏蔽成功", "取消屏蔽");
    }
}
