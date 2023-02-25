package com.socket.server.controller.login;

import com.socket.core.model.enums.HttpStatus;
import com.socket.server.request.vo.QQAuthReq;
import com.socket.server.service.impl.QQLoginServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qqlogin")
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
