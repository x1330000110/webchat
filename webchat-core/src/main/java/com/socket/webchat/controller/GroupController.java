package com.socket.webchat.controller;

import com.socket.webchat.model.condition.GroupCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final SysGroupService sysGroupService;

    @PostMapping("/join")
    public HttpStatus joinGroup(@RequestBody GroupCondition condition) {
        boolean b = sysGroupService.joinGroup(condition.getGroupId(), Wss.getUserId());
        return HttpStatus.of(b, "加入成功", "您已经是该群组成员");
    }

    @PostMapping("/exit")
    public HttpStatus exitGroup(@RequestBody GroupCondition condition) {
        boolean b = sysGroupService.exitGroup(condition.getGroupId());
        return HttpStatus.of(b, "退出成功", "找不到相关信息");
    }

    @PostMapping("/create")
    public HttpStatus createGroup(@RequestBody GroupCondition condition) {
        String gid = sysGroupService.createGroup(condition.getGroupName(), condition.getGroupImg());
        return HttpStatus.SUCCESS.body("创建成功", gid);
    }

    @PostMapping("/remove")
    public HttpStatus removeUser(@RequestBody GroupCondition condition) {
        boolean b = sysGroupService.removeUser(Wss.getUserId(), condition.getGroupId(), condition.getUid());
        return HttpStatus.of(b, "移除群组用户成功", "找不到相关信息");
    }

    @PostMapping("/dissolve")
    public HttpStatus dissolve(@RequestBody GroupCondition condition) {
        boolean b = sysGroupService.dissolveGroup(condition.getGroupId());
        return HttpStatus.of(b, "群组解散成功", "找不到相关信息");
    }
}
