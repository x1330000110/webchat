package com.socket.secure.event;

import com.socket.secure.event.entity.KeyEvent;
import com.socket.secure.util.AES;
import org.springframework.context.ApplicationListener;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * By implementing the {@link KeyExchangeListener} interface,
 * Spring will push the <b>AES key</b> when the key exchange is complete.
 * or, you can also obtain the AES key through {@linkplain AES#getAesKey(HttpSession)}.
 * If you want to store this key and session in a separate mapping space,
 * it is recommended to implement the {@linkplain #onSessionClosed} method
 * and perform corresponding cleanup operations when the session is invalid
 *
 * @see ApplicationListener
 * @date 2021/10/8
 */
public interface KeyExchangeListener extends ApplicationListener<KeyEvent>, HttpSessionListener {
    @Override
    default void onApplicationEvent(KeyEvent event) {
        onKeyExchange(event.getSession(), event.getKey());
    }

    @Override
    default void sessionDestroyed(HttpSessionEvent event) {
        onSessionClosed(event.getSession());
    }

    /**
     * Push AES key when key exchange is successful
     *
     * @param session {@linkplain HttpSession}
     * @param key     AES KEY
     */
    void onKeyExchange(HttpSession session, String key);

    /**
     * Fired when the session is closed
     *
     * @param session {@linkplain HttpSession}
     */
    void onSessionClosed(HttpSession session);
}
