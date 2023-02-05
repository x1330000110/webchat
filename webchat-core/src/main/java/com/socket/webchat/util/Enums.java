package com.socket.webchat.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;

import java.util.Arrays;

/**
 * 枚举工具类
 */
public class Enums {
    /**
     * 枚举转JSON
     */
    public static <E extends Enum<E>> String toJSON(E e) {
        JSONObject json = new JSONObject();
        BeanUtil.descForEach(e.getClass(), prop -> json.set(prop.getFieldName(), prop.getValue(e)));
        return json.toString();
    }

    /**
     * 字符串匹配枚举
     */
    public static <E extends Enum<E>> E of(Class<E> enumClass, String value) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> key(e).equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取Enum标准KEY以字符串的表现形式
     */
    public static <E extends Enum<E>> String key(E item) {
        return StrUtil.toCamelCase(item.name().toLowerCase());
    }
}
