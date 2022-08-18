package com.socket.webchat.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.secure.runtime.InvalidRequestException;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.model.condition.PasswordCondition;
import com.socket.webchat.model.condition.RegisterCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.service.SysUserService;
import com.socket.webchat.service.WxloginService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.RedisValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LoginController {
    private static final String CLIENT_URL = "https://f12.ink";
    private static final String SUCCESS = CLIENT_URL + "/status/success.html";
    private static final String EXPIRED = CLIENT_URL + "/status/failed.html?key=expired";
    private static final String LOCK = CLIENT_URL + "/status/failed.html?key=lock";

    private final RedisTemplate<String, Integer> template;
    private final SysUserService sysUserService;
    private final WxloginService wxloginService;

    @PostMapping("/wxstate/{uuid}")
    public HttpStatus wxstate(@PathVariable String uuid) {
        boolean state = wxloginService.login(uuid);
        return state ? HttpStatus.SUCCESS.message("登录成功") : HttpStatus.WAITTING.message("等待访问");
    }

    @GetMapping("/wxqrcode/{uuid}")
    public void wxqrcode(HttpServletResponse response, @PathVariable String uuid) {
        wxloginService.generatePiccode(response, uuid);
    }

    @GetMapping("/wxfasturl/{uuid}")
    public HttpStatus wxfasturl(@PathVariable String uuid) {
        // 获取快捷登录链接 (微信浏览器)
        String base64url = Base64.encode(wxloginService.getWxFastUrl(uuid));
        return HttpStatus.SUCCESS.body(base64url);
    }

    @CrossOrigin
    @GetMapping("/wxlogin")
    public void wxlogin(String code, String state, HttpServletResponse response) throws IOException {
        response.setContentType(ContentType.TEXT_HTML.toString(StandardCharsets.UTF_8));
        SysUser user = wxloginService.authorize(code, state);
        boolean wxMobile = state.endsWith(Constants.WX_MOBILE);
        if (user != null) {
            // 微信浏览器直接跳转标识
            if (wxMobile) {
                // 永久限制登录
                if (user.isDeleted()) {
                    response.sendRedirect(LOCK);
                    return;
                }
                // 临时限制登录
                long time = RedisValue.of(template, RedisTree.LOCK.getPath(user.getUid())).getExpired();
                if (time > 0) {
                    response.sendRedirect(LOCK + "&time=" + time);
                    return;
                }
                wxloginService.login(state);
                response.sendRedirect(CLIENT_URL);
                return;
            }
            // 手机扫码登录处理
            response.sendRedirect(SUCCESS);
            return;
        }
        response.sendRedirect(EXPIRED);
    }

    @Encrypted
    @GetMapping("/login")
    public HttpStatus login(LoginCondition condition) {
        // 必要的密码长度判断
        Assert.isTrue(StrUtil.length(condition.getPass()) <= 16, InvalidRequestException::new);
        sysUserService.login(condition);
        return HttpStatus.SUCCESS.message("登录成功");
    }

    @Encrypted
    @PostMapping("/register")
    public HttpStatus register(@RequestBody RegisterCondition condition) {
        sysUserService.register(condition);
        return HttpStatus.SUCCESS.message("注册成功");
    }

    @Encrypted
    @PostMapping("/send")
    public HttpStatus send(@RequestBody EmailCondition condition) {
        String email = sysUserService.sendEmail(condition.getUser());
        return HttpStatus.SUCCESS.body("发送成功", email);
    }

    @Encrypted
    @PostMapping("/password")
    public HttpStatus updatePassword(@RequestBody PasswordCondition condition) {
        boolean success = sysUserService.updatePassword(condition);
        return HttpStatus.state(success, "修改");
    }
}

