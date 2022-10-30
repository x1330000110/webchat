package com.socket.webchat.controller;

import com.socket.secure.filter.anno.Encrypted;
import com.socket.webchat.constant.Announce;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
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
        RedisMap<String, String> map = redis.withMap(RedisTree.ANNOUNCE.get());
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
