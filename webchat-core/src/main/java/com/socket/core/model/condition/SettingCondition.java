package com.socket.core.model.condition;

import cn.hutool.core.util.EnumUtil;
import com.socket.core.model.enums.Setting;
import lombok.Setter;

@Setter
public class SettingCondition {
    private String key;

    public Setting getSetting() {
        return EnumUtil.likeValueOf(Setting.class, key);
    }
}
