package com.socket.server.controller;

import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.request.QQAuthReq;
import com.socket.server.service.impl.QQLoginServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qq")
public class QQLoginController {
    private final QQLoginServiceImpl qqLoginService;

    @GetMapping("/auth")
    public HttpStatus auth() {
        QQAuthReq auth = qqLoginService.getLoginAuth();
        return HttpStatus.SUCCESS.body(auth);
    }

    @PostMapping("/state/{qrsig}")
    public HttpStatus state(@PathVariable String qrsig) {
        return qqLoginService.state(qrsig);
    }
}
