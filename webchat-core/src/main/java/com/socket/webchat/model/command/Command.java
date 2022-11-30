package com.socket.webchat.model.command;

/**
 * 命令枚举
 */
public interface Command<E extends Enum<E>> {

    default String getName() {
        //noinspection unchecked
        return getClass().getSimpleName() + '.' + ((Enum<E>) this).name().toLowerCase();
    }
}
