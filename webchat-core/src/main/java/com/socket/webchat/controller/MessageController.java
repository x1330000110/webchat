package com.socket.webchat.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.AES;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.condition.MessageCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @date 2022/6/22
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/message")
public class MessageController {
    private final RecordService recordService;

    @Encrypted
    @PostMapping("/clear")
    public HttpStatus clear(@RequestBody MessageCondition condition) {
        recordService.removeAllMessage(Wss.getUserId(), condition.getTarget());
        return HttpStatus.SUCCESS.message("清除成功");
    }

    @Encrypted
    @PostMapping("/reading")
    public HttpStatus reading(@RequestBody MessageCondition condition) {
        if (condition.getMid() != null) {
            recordService.readMessage(condition.getMid(), condition.getTarget());
        } else {
            recordService.readAllMessage(Wss.getUserId(), condition.getTarget(), true);
        }
        return HttpStatus.SUCCESS.message("操作成功");
    }

    @Encrypted
    @PostMapping("/remove")
    public HttpStatus remove(@RequestBody MessageCondition condition) {
        boolean state = recordService.removeMessageWithSelf(condition.getMid());
        return HttpStatus.of(state, "操作成功", "权限不足");
    }

    @Encrypted
    @PostMapping("/deleteMessage")
    public HttpStatus deleteMessage(@RequestBody MessageCondition condition) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::getMid, condition.getMid());
        // 所有者可直接移除消息
        if (Wss.getUser().getRole() == UserRole.OWNER) {
            boolean state = recordService.remove(wrapper);
            return HttpStatus.of(state, "操作成功", "找不到相关记录");
        }
        return HttpStatus.UNAUTHORIZED.body("权限不足");
    }

    @GetMapping
    public HttpStatus records(String mid, String target, HttpSession session) {
        List<ChatRecord> list = recordService.getRecords(mid, target);
        // 加密消息
        List<String> collect = list.stream()
                .map(JSONUtil::toJsonStr)
                .map(json -> AES.encrypt(json, session))
                .collect(Collectors.toList());
        return HttpStatus.SUCCESS.body(collect);
    }
}
