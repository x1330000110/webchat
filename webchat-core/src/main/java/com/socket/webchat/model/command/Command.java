package com.socket.webchat.model.command;

import cn.hutool.core.util.StrUtil;

/**
 * 命令枚举
 */
public interface Command<E extends Enum<E>> {

    /**
     * 获取对外开放的完整命令名
     */
    default String getCommand() {
        return getClass().getSimpleName() + '.' + getImplName();
    }

    /**
     * 获取命令枚举对应实现类名
     */
    default String getImplName() {
        //noinspection unchecked
        return StrUtil.toCamelCase(((Enum<E>) this).name().toLowerCase());
    }
}
