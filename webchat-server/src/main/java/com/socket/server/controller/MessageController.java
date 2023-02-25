package com.socket.server.controller;

import cn.hutool.json.JSONUtil;
import com.socket.core.model.condition.MessageCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.po.ChatRecord;
import com.socket.core.service.ChatRecordService;
import com.socket.core.util.ShiroUser;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.AES;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/message")
public class MessageController {
    private final ChatRecordService chatRecordService;

    @Encrypted
    @PostMapping("/clear")
    public HttpStatus clear(@RequestBody MessageCondition condition) {
        chatRecordService.removeAllMessage(condition.getTarget());
        return HttpStatus.SUCCESS.message("清除成功");
    }

    @Encrypted
    @PostMapping("/reading")
    public HttpStatus reading(@RequestBody MessageCondition condition) {
        if (condition.getMid() != null) {
            chatRecordService.readMessage(condition.getMid(), condition.getTarget());
        } else {
            chatRecordService.readAllMessage(ShiroUser.getUserId(), condition.getTarget(), true);
        }
        return HttpStatus.SUCCESS.message("操作成功");
    }

    @Encrypted
    @PostMapping("/withdraw")
    public HttpStatus withdrawMessage(@RequestBody MessageCondition condition) {
        boolean state = chatRecordService.withdrawMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @Encrypted
    @PostMapping("/remove")
    public HttpStatus removeMessage(@RequestBody MessageCondition condition) {
        boolean state = chatRecordService.removeMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @GetMapping("")
    public HttpStatus records(String mid, String target, HttpSession session) {
        List<ChatRecord> list = chatRecordService.getRecords(mid, target);
        // 加密消息
        List<String> collect = list.stream()
                .map(JSONUtil::toJsonStr)
                .map(json -> AES.encrypt(json, session))
                .collect(Collectors.toList());
        return HttpStatus.SUCCESS.body(collect);
    }
}
