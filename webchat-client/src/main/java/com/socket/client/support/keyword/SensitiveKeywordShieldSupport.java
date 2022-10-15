package com.socket.client.support.keyword;

import cn.hutool.extra.pinyin.PinyinUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 敏感关键词屏蔽支持
 */
@Component
public class SensitiveKeywordShieldSupport {
    private static final String[] pinyins = {
            "sima",
            "nima",
            "siquanjia",
            "meimu",
            "guer",
            "zhizhang",
            "naotan",
            "shabi",
            "wori",
            "nm",
            "sb",
            "nt",
            "nc"
    };

    /**
     * 检查指定字符串是否包含敏感关键词
     *
     * @param str 字符串
     * @return 包含返回true
     */
    public boolean containsSensitive(String str) {
        String pinyin = PinyinUtil.getPinyin(str, "");
        return Arrays.stream(pinyins).anyMatch(pinyin::contains);
    }
}
