package com.socket.server.controller;

import cn.hutool.json.JSONUtil;
import com.socket.core.constant.Constants;
import com.socket.core.model.condition.MessageCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.po.ChatRecord;
import com.socket.secure.util.AES;
import com.socket.server.custom.filter.anno.OpenApi;
import com.socket.server.service.ChatRecordService;
import com.socket.server.util.ShiroUser;
import com.socket.server.util.servlet.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/message")
public class MessageController {
    private final ChatRecordService chatRecordService;

    @PostMapping("/clear")
    public HttpStatus clear(@RequestBody MessageCondition condition) {
        chatRecordService.removeAllMessage(condition.getTarget());
        return HttpStatus.SUCCESS.message("清除成功");
    }

    @PostMapping("/reading")
    public HttpStatus reading(@RequestBody MessageCondition condition) {
        if (condition.getMid() != null) {
            chatRecordService.readMessage(condition.getMid(), condition.getTarget());
        } else {
            boolean audio = Boolean.TRUE.equals(condition.getAudio());
            chatRecordService.readAllMessage(ShiroUser.getUserId(), condition.getTarget(), audio);
        }
        return HttpStatus.SUCCESS.message("操作成功");
    }

    @PostMapping("/withdraw")
    public HttpStatus withdrawMessage(@RequestBody MessageCondition condition) {
        boolean state = chatRecordService.withdrawMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @PostMapping("/remove")
    public HttpStatus removeMessage(@RequestBody MessageCondition condition) {
        boolean state = chatRecordService.removeMessage(condition.getMid());
        return HttpStatus.state(state, "操作");
    }

    @GetMapping("/")
    public HttpStatus records(String mid, String target, HttpSession session) {
        List<ChatRecord> list = chatRecordService.getRecords(mid, target);
        // 加密消息
        List<String> collect = list.stream()
                .map(JSONUtil::toJsonStr)
                .map(json -> AES.encrypt(json, session))
                .collect(Collectors.toList());
        return HttpStatus.SUCCESS.body(collect);
    }

    @OpenApi
    @GetMapping("/latest")
    public HttpStatus getLatest() {
        String userId = Request.get(Constants.CURRENT_USER_ID);
        Map<String, ChatRecord> map = chatRecordService.getLatestUnreadMessages(userId);
        return HttpStatus.SUCCESS.body(map);
    }
}
