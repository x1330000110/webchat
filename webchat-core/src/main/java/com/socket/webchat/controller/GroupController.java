package com.socket.webchat.controller;

import com.socket.webchat.model.condition.GroupCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final SysGroupService sysGroupService;

    @PostMapping("/join")
    public HttpStatus joinGroup(GroupCondition condition) {
        boolean b = sysGroupService.joinGroup(condition.getGroupId(), Wss.getUserId());
        return HttpStatus.state(b, "加入");
    }

    @PostMapping("/exit")
    public HttpStatus exitGroup(GroupCondition condition) {
        boolean b = sysGroupService.exitGroup(condition.getGroupId());
        return HttpStatus.state(b, "退出");
    }

    @PostMapping("/create")
    public HttpStatus createGroup(GroupCondition condition) {
        boolean b = sysGroupService.createGroup(condition.getGroupName());
        return HttpStatus.state(b, "创建");
    }

    @PostMapping("/remove")
    public HttpStatus removeUser(GroupCondition condition) {
        boolean b = sysGroupService.removeUser(Wss.getUserId(), condition.getGroupId(), condition.getUid());
        return HttpStatus.state(b, "移除");
    }

    @PostMapping("/dissolve")
    public HttpStatus dissolve(GroupCondition condition) {
        boolean b = sysGroupService.dissolveGroup(condition.getGroupId());
        return HttpStatus.state(b, "解散");
    }
}
