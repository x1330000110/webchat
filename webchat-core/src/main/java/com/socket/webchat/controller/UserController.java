package com.socket.webchat.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.UserCondition;
import com.socket.webchat.model.enums.Announce;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.RedisValue;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final RedisTemplate<String, Object> template;
    private final SysUserService service;

    @Encrypted
    @PostMapping(value = "/material")
    public HttpStatus material(@RequestBody SysUser sysUser) {
        boolean success = service.updateMaterial(sysUser);
        return HttpStatus.state(success, "修改");
    }

    @Encrypted
    @PostMapping("/avatar")
    public HttpStatus updateAvatar(MultipartFile blob) throws IOException {
        String mapping = service.updateAvatar(blob.getBytes());
        return HttpStatus.SUCCESS.body("修改成功", mapping);
    }

    @Encrypted
    @PostMapping("/email")
    public HttpStatus updateEmail(@RequestBody EmailCondition condition) {
        boolean success = service.updateEmail(condition);
        return HttpStatus.state(success, "修改");
    }

    @PostMapping("/logout")
    public HttpStatus logout() {
        SecurityUtils.getSubject().logout();
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/{uid}")
    public HttpStatus userInfo(@PathVariable String uid) {
        SysUser user = service.getUserInfo(uid);
        return HttpStatus.SUCCESS.body(user);
    }

    @Encrypted
    @PostMapping("/remove")
    public HttpStatus removeUser(@RequestBody UserCondition condition) {
        if (Wss.getUser().getRole() != UserRole.OWNER) {
            return HttpStatus.UNAUTHORIZED.message("权限不足");
        }
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getUid, condition.getUid());
        wrapper.eq(SysUser::isDeleted, 0);
        wrapper.set(SysUser::isDeleted, 1);
        return HttpStatus.of(service.update(wrapper), "操作成功", "找不到此用户");
    }

    @GetMapping("/notice")
    public HttpStatus getNotice(String digest) {
        RedisMap<String, Object> map = RedisValue.ofMap(template, RedisTree.ANNOUNCEMENT.getPath());
        Object dg = map.get(Announce.digest.string());
        // 散列id不同 表示发布新内容
        if (dg != null && !dg.equals(digest)) {
            String kc = Announce.content.string();
            String kt = Announce.time.string();
            Map<String, Object> json = Map.of(kc, map.get(kc), kt, map.get(kt));
            return HttpStatus.SUCCESS.body(json);
        }
        return HttpStatus.FAILURE.body();
    }
}
