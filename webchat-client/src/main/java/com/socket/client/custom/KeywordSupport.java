package com.socket.client.custom;

import cn.hutool.extra.pinyin.PinyinUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 敏感关键词屏蔽支持
 */
@Component
public class KeywordSupport {
    /**
     * 字母屏蔽
     */
    private static final String[] codes = {
            "sb", "nm", "sm"
    };
    /**
     * 汉字屏蔽
     */
    private static final String[] pinyins = {
            "si ma", "ni ma", "si quan jia", "mei mu",
            "gu er", "zhi zhang", "nao tan",
            "sha bi", "wo ri", "hu kou"
    };

    /**
     * 检查指定字符串是否包含敏感关键词
     *
     * @param str 字符串
     * @return 包含返回true
     */
    public boolean containsSensitive(String str) {
        if (Arrays.stream(codes).anyMatch(str::contains)) {
            return true;
        }
        String pinyin = PinyinUtil.getPinyin(str, " ");
        return Arrays.stream(pinyins).anyMatch(pinyin::contains);
    }
}
