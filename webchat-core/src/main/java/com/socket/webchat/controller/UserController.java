package com.socket.webchat.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.webchat.constant.Announce;
import com.socket.webchat.custom.RedisClient;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.UserCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final SysUserService sysUserService;
    private final RedisClient<String> redisClient;

    @Encrypted
    @PostMapping(value = "/material")
    public HttpStatus material(@RequestBody SysUser sysUser) {
        SysUser user = sysUserService.updateMaterial(sysUser);
        Optional.ofNullable(user).ifPresent(u -> Wss.updatePrincipal(Wss.getUser()::setName, u.getName()));
        return HttpStatus.state(user != null, "修改");
    }

    @Encrypted
    @PostMapping("/avatar")
    public HttpStatus updateAvatar(MultipartFile blob) throws IOException {
        String mapping = sysUserService.updateAvatar(blob.getBytes());
        if (mapping == null) {
            return HttpStatus.FAILURE.message("修改失败");
        }
        Wss.updatePrincipal(Wss.getUser()::setHeadimgurl, mapping);
        return HttpStatus.SUCCESS.body("修改成功", mapping);
    }

    @Encrypted
    @PostMapping("/email")
    public HttpStatus updateEmail(@RequestBody EmailCondition condition) {
        String email = sysUserService.updateEmail(condition);
        Optional.ofNullable(email).ifPresent(e -> Wss.updatePrincipal(Wss.getUser()::setEmail, e));
        return HttpStatus.state(email != null, "修改");
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

    @Encrypted
    @PostMapping("/remove")
    public HttpStatus removeUser(@RequestBody UserCondition condition) {
        if (Wss.getUser().getRole() != UserRole.OWNER) {
            return HttpStatus.UNAUTHORIZED.message("权限不足");
        }
        LambdaUpdateWrapper<SysUser> wrapper = Wss.lambdaUpdate();
        wrapper.eq(SysUser::getUid, condition.getUid());
        wrapper.set(SysUser::isDeleted, 1);
        return HttpStatus.of(sysUserService.update(wrapper), "操作成功", "找不到此用户");
    }

    @GetMapping("/notice")
    public HttpStatus getNotice(String digest) {
        RedisMap<String, String> map = redisClient.withMap(RedisTree.ANNOUNCE.concat());
        Object dg = map.get(Announce.digest);
        // 散列id不同 表示发布新内容
        if (dg != null && !dg.equals(digest)) {
            String content = Announce.content, time = Announce.time;
            Map<String, Object> json = Map.of(content, map.get(content), time, map.get(time));
            return HttpStatus.SUCCESS.body(json);
        }
        return HttpStatus.FAILURE.body();
    }
}
