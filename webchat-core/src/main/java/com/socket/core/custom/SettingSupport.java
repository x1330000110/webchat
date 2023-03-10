package com.socket.core.custom;

import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.enums.Setting;
import com.socket.core.util.RedisClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.stereotype.Component;

/**
 * 聊天室所有者部分权限Redis同步支持
 */
@Component
public class SettingSupport {
    @Getter
    private RedisMap<String, Boolean> map;

    @Autowired
    public void setMap(RedisClient<Boolean> client) {
        this.map = client.withMap(RedisTree.SETTING.get());
    }

    /**
     * 切换设置
     *
     * @param setting 设置
     */
    public void switchSetting(Setting setting) {
        map.put(setting.getKey(), !getSetting(setting));
    }

    /**
     * 获取设置
     *
     * @param setting 设置
     * @return 启用返回true
     */
    public boolean getSetting(Setting setting) {
        return map.getOrDefault(setting.getKey(), false);
    }
}
