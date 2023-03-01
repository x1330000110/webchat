package com.socket.server.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.core.constant.ChatConstants;
import com.socket.core.model.condition.GroupCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.po.SysGroup;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.Assert;
import com.socket.server.service.SysGroupService;
import com.socket.server.util.ShiroUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final SysGroupService sysGroupService;
    private final ChatConstants constants;

    @Encrypted
    @PostMapping("/join")
    public HttpStatus joinGroup(@RequestBody GroupCondition condition) {
        String password = condition.getPassword();
        List<String> uids = sysGroupService.joinGroup(condition.getGid(), ShiroUser.getUserId(), password);
        return HttpStatus.SUCCESS.body("加入成功", uids);
    }

    @PostMapping("/exit")
    public HttpStatus exitGroup(@RequestBody GroupCondition condition) {
        String gid = condition.getGid();
        Assert.isFalse(constants.getDefaultGroup().equals(gid), "无法退出默认群组", IllegalStateException::new);
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
        Assert.isFalse(constants.getDefaultGroup().equals(gid), "无法移除默认群组用户", IllegalStateException::new);
        boolean b = sysGroupService.removeUser(gid, condition.getUid());
        return HttpStatus.of(b, "移除群组用户成功", "找不到相关信息");
    }

    @PostMapping("/dissolve")
    public HttpStatus dissolve(@RequestBody GroupCondition condition) {
        String gid = condition.getGid();
        Assert.isFalse(constants.getDefaultGroup().equals(gid), "无法解散默认群组", IllegalStateException::new);
        boolean b = sysGroupService.dissolveGroup(gid);
        return HttpStatus.of(b, "群组解散成功", "找不到相关信息");
    }

    @GetMapping("/list")
    public HttpStatus list(GroupCondition condition) {
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        String gid = condition.getGid();
        wrapper.like(StrUtil.isNotEmpty(gid), SysGroup::getGuid, gid);
        List<SysGroup> list = sysGroupService.list(wrapper);
        list.removeIf(group -> constants.getDefaultGroup().equals(group.getGuid()));
        // 移除密码
        for (SysGroup group : list) {
            group.setNeedPass(StrUtil.isNotEmpty(group.getPassword()));
            group.setPassword(null);
        }
        return HttpStatus.SUCCESS.body(list);
    }

    @Encrypted
    @PostMapping("/updatePass")
    public HttpStatus updatePassword(@RequestBody GroupCondition condition) {
        boolean state = sysGroupService.updatePassword(condition.getGid(), condition.getPassword());
        return HttpStatus.state(state, "操作");
    }
}
