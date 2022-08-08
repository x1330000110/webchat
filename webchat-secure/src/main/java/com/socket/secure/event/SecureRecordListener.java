package com.socket.secure.event;

import com.socket.secure.event.entity.InitiatorEvent;
import org.springframework.context.ApplicationListener;

/**
 * By implementing the {@link SecureRecordListener} interface,
 * The filter logs and pushes the request originator's information when unauthorized access to the protected controller
 */
public interface SecureRecordListener extends ApplicationListener<InitiatorEvent> {
    default void onApplicationEvent(InitiatorEvent event) {
        onInterceptEvent(event);
    }

    /**
     * Trigger an event when an illegal operation is blocked
     *
     * @param event {@link InitiatorEvent}
     */
    void onInterceptEvent(InitiatorEvent event);
}
