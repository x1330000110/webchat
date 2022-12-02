package com.socket.webchat.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.condition.GroupCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final SysGroupService sysGroupService;

    @Encrypted
    @PostMapping("/join")
    public HttpStatus joinGroup(@RequestBody GroupCondition condition) {
        String password = condition.getPassword();
        List<String> uids = sysGroupService.joinGroup(condition.getGid(), Wss.getUserId(), password);
        return HttpStatus.SUCCESS.body("加入成功", uids);
    }

    @PostMapping("/exit")
    public HttpStatus exitGroup(@RequestBody GroupCondition condition) {
        String gid = condition.getGid();
        Assert.isFalse(Constants.GROUP.equals(gid), "无法退出默认群组", IllegalStateException::new);
        boolean b = sysGroupService.exitGroup(gid);
        return HttpStatus.of(b, "退出成功", "找不到相关信息");
    }

    @Encrypted
    @PostMapping("/create")
    public HttpStatus createGroup(@RequestBody GroupCondition condition) {
        String gid = sysGroupService.createGroup(condition.getGroupName(), condition.getPassword());
        return HttpStatus.SUCCESS.body("创建成功", gid);
    }

    @PostMapping("/remove")
    public HttpStatus removeUser(@RequestBody GroupCondition condition) {
        String gid = condition.getGid();
        Assert.isFalse(Constants.GROUP.equals(gid), "无法移除默认群组用户", IllegalStateException::new);
        boolean b = sysGroupService.removeUser(gid, condition.getUid());
        return HttpStatus.of(b, "移除群组用户成功", "找不到相关信息");
    }

    @PostMapping("/dissolve")
    public HttpStatus dissolve(@RequestBody GroupCondition condition) {
        String gid = condition.getGid();
        Assert.isFalse(Constants.GROUP.equals(gid), "无法解散默认群组", IllegalStateException::new);
        boolean b = sysGroupService.dissolveGroup(gid);
        return HttpStatus.of(b, "群组解散成功", "找不到相关信息");
    }

    @GetMapping("/list")
    public HttpStatus list(GroupCondition condition) {
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        String gid = condition.getGid();
        wrapper.like(StrUtil.isNotEmpty(gid), SysGroup::getGuid, gid);
        List<SysGroup> list = sysGroupService.list(wrapper);
        list.removeIf(group -> Constants.GROUP.equals(group.getGuid()));
        // 移除密码
        for (SysGroup group : list) {
            group.setNeedPass(StrUtil.isNotEmpty(group.getPassword()));
            group.setPassword(null);
        }
        return HttpStatus.SUCCESS.body(list);
    }
}
