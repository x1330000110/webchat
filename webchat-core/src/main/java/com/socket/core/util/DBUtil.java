package com.socket.core.util;

import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.StrUtil;

/**
 * mybatis plus扩展工具类
 */
public class DBUtil {
    /**
     * mybatis group by max合成字符串
     */
    public static <T> String selectMax(Func1<T, ?> lambda) {
        String column = columnToString(lambda);
        return StrUtil.format("MAX({}) AS {}", column, column);
    }

    /**
     * 获取指定函数式接口命名形式
     */
    public static <T> String columnToString(Func1<T, ?> lambda) {
        return StrUtil.toUnderlineCase(LambdaUtil.getFieldName(lambda));
    }

    /**
     * mybatis distinct合成字符串（注意 这个方法只能调用一次）
     */
    public static <T> String selectDistinct(Func1<T, ?> lambda) {
        String column = columnToString(lambda);
        return StrUtil.format("DISTINCT {}", column, column);
    }
}
