package com.socket.core.model.command;

import cn.hutool.core.lang.EnumItem;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 * 命令枚举（兼容JSON序列化）
 */
public interface Command<E extends Command<E> & EnumItem<E>> extends EnumItem<E> {
    /**
     * 获取对外开放的完整命令名
     */
    default String getOpenName() {
        return getClass().getSimpleName() + '.' + getName();
    }

    /**
     * 获取命令枚举对应实现类名
     */
    default String getName() {
        return StrUtil.toCamelCase((name().toLowerCase()));
    }

    String name();

    default int intVal() {
        return ordinal();
    }

    int ordinal();

    @Override
    default E fromStr(String str) {
        return Arrays.stream(items())
                .filter(em -> Objects.equals(em.toString(), str))
                .findFirst()
                .orElse(null);
    }
}
