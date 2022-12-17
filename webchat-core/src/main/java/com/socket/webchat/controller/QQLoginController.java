package com.socket.webchat.controller;

import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.request.bean.QQAuth;
import com.socket.webchat.service.impl.QQLoginServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qq")
public class QQLoginController {
    private final QQLoginServiceImpl qqLoginService;

    @GetMapping("/auth")
    public HttpStatus auth() {
        QQAuth auth = qqLoginService.getLoginAuth();
        return HttpStatus.SUCCESS.body(auth);
    }

    @PostMapping("/state/{qrsig}")
    public HttpStatus state(@PathVariable String qrsig) {
        return qqLoginService.state(qrsig);
    }
}
