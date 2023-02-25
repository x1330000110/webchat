package com.socket.server.controller.login;

import cn.hutool.core.util.StrUtil;
import com.socket.core.model.condition.EmailCondition;
import com.socket.core.model.condition.LoginCondition;
import com.socket.core.model.condition.PasswordCondition;
import com.socket.core.model.condition.RegisterCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.util.Assert;
import com.socket.server.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Encrypted
@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class LoginController {
    private final SysUserService sysUserService;

    @GetMapping("/login")
    public HttpStatus login(LoginCondition condition) {
        // 必要的密码长度判断
        Assert.isTrue(StrUtil.length(condition.getPass()) <= 16, "无效的请求", InvalidRequestException::new);
        sysUserService.login(condition);
        return HttpStatus.SUCCESS.message("登录成功");
    }

    @PostMapping("/register")
    public HttpStatus register(@RequestBody RegisterCondition condition) {
        sysUserService.register(condition);
        return HttpStatus.SUCCESS.message("注册成功");
    }

    @PostMapping("/send")
    public HttpStatus send(@RequestBody EmailCondition condition) {
        String email = sysUserService.sendEmail(condition.getEmail());
        return HttpStatus.SUCCESS.body("发送成功", email);
    }

    @PostMapping("/password")
    public HttpStatus updatePassword(@RequestBody PasswordCondition condition) {
        boolean success = sysUserService.updatePassword(condition);
        return HttpStatus.state(success, "修改");
    }
}

