package com.socket.webchat.model.condition;

import com.socket.webchat.model.enums.Setting;
import lombok.Setter;

import java.util.Arrays;

@Setter
public class SettingCondition {
    private String key;

    public Setting getSetting() {
        return Arrays.stream(Setting.values())
                .filter(e -> e.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("无效参数：" + key));
    }
}
