package com.socket.webchat.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.ContentType;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.constant.properties.WxProperties;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.request.WeChatRedirect;
import com.socket.webchat.service.WxloginService;
import com.socket.webchat.util.RedirectUtil;
import com.socket.webchat.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat")
public class WxLoginController {
    private final WxloginService wxloginService;
    private final WxProperties properties;
    private final RedisClient<?> redis;

    @PostMapping("/state/{uuid}")
    public HttpStatus state(@PathVariable String uuid) {
        boolean state = wxloginService.login(uuid);
        return state ? HttpStatus.SUCCESS.message("登录成功") : HttpStatus.WAITTING.message("等待访问");
    }

    @GetMapping("/qrcode/{uuid}")
    public void qrcode(HttpServletResponse response, @PathVariable String uuid) {
        wxloginService.generatePiccode(response, uuid);
    }

    @GetMapping("/fasturl/{uuid}")
    public HttpStatus fasturl(@PathVariable String uuid) {
        // 获取快捷登录链接 (微信浏览器)
        String base64url = Base64.encode(wxloginService.getWxFastUrl(uuid));
        return HttpStatus.SUCCESS.body(base64url);
    }

    @WeChatRedirect("/login")
    public void login(String code, String state, HttpServletResponse response) {
        String domain = properties.getDomainService();
        response.setContentType(ContentType.TEXT_HTML.toString(StandardCharsets.UTF_8));
        SysUser user = wxloginService.authorize(code, state);
        boolean wxMobile = state.endsWith(Constants.WX_MOBILE);
        // 二维码过期
        RedirectUtil.redirectIfNull(user, response, domain + "/status/failed.html?key=expired");
        // 永久限制登录
        RedirectUtil.redirectIf(user.isDeleted(), response, domain + "/status/failed.html?key=lock");
        // 临时限制登录
        long time = redis.getExpired(RedisTree.LOCK.concat(user.getGuid()));
        RedirectUtil.redirectIf(time > 0, response, domain + "/status/failed.html?key=lock&time=" + time);
        // 手机扫码登录处理
        RedirectUtil.redirectIf(!wxMobile, response, domain + "/status/success.html");
        // 扫码登录
        wxloginService.login(state);
        RedirectUtil.redirect(response, domain);
    }
}
