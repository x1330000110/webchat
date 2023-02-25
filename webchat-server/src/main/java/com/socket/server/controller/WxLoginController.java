package com.socket.server.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.ContentType;
import com.socket.core.config.properties.WxProperties;
import com.socket.core.constant.Constants;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.po.SysUser;
import com.socket.core.util.RedisClient;
import com.socket.server.request.anno.WeChatRedirect;
import com.socket.server.service.WxloginService;
import com.socket.server.util.RedirectUtil;
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
        String url = properties.getRedirect();
        response.setContentType(ContentType.TEXT_HTML.toString(StandardCharsets.UTF_8));
        SysUser user = wxloginService.authorize(code, state);
        boolean wxMobile = state.endsWith(Constants.WX_MOBILE);
        // 二维码过期
        RedirectUtil.redirectIfNull(user, response, url + "/status/failed.html?key=expired");
        // 永久限制登录
        RedirectUtil.redirectIf(user.isDeleted(), response, url + "/status/failed.html?key=lock");
        // 临时限制登录
        long time = redis.getExpired(RedisTree.LOCK.concat(user.getGuid()));
        RedirectUtil.redirectIf(time > 0, response, url + "/status/failed.html?key=lock&time=" + time);
        // 手机扫码登录处理
        RedirectUtil.redirectIf(!wxMobile, response, url + "/status/success.html");
        // 扫码登录
        wxloginService.login(state);
        RedirectUtil.redirect(response, url);
    }
}
