package com.socket.server.custom;

import com.socket.core.constant.Constants;
import com.socket.core.custom.TokenUserManager;
import com.socket.secure.event.KeyExchangeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

/**
 * 密钥监控事件与缓存同步支持
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionKeySupport implements KeyExchangeListener {
    private final TokenUserManager tokenUserManager;

    @Override
    public void onKeyExchange(HttpSession session, String key) {
        String token = (String) session.getAttribute(Constants.AUTH_TOKEN);
        if (token != null) {
            tokenUserManager.setEncKey(token, key);
        }
    }

    @Override
    public void onSessionClosed(HttpSession session) {
        String token = (String) session.getAttribute(Constants.AUTH_TOKEN);
        tokenUserManager.removeUser(token);
    }
}
