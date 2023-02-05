package com.socket.webchat.controller;

import cn.hutool.json.JSONUtil;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.AES;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.condition.MessageCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.util.ShiroUser;
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
        recordService.removeAllMessage(condition.getTarget());
        return HttpStatus.SUCCESS.message("清除成功");
    }

    @Encrypted
    @PostMapping("/reading")
    public HttpStatus reading(@RequestBody MessageCondition condition) {
        if (condition.getMid() != null) {
            recordService.readMessage(condition.getMid(), condition.getTarget());
        } else {
            recordService.readAllMessage(ShiroUser.getUserId(), condition.getTarget(), true);
        }
        return HttpStatus.SUCCESS.message("操作成功");
    }

    @Encrypted
    @PostMapping("/withdraw")
    public HttpStatus withdrawMessage(@RequestBody MessageCondition condition) {
        boolean state = recordService.withdrawMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @Encrypted
    @PostMapping("/remove")
    public HttpStatus removeMessage(@RequestBody MessageCondition condition) {
        boolean state = recordService.removeMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @GetMapping("")
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
