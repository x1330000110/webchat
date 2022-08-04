package com.socket.client.manager;

import cn.hutool.crypto.digest.MD5;
import com.socket.client.model.enums.SocketTree;
import com.socket.webchat.model.enums.Announce;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.util.RedisValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Socket Redis 管理
 */
@Component
public class RedisManager {
    private RedisTemplate<String, Object> template;

    @Autowired
    public void setTemplate(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    /**
     * 临时禁言
     *
     * @param uid  目标用户
     * @param time 时间（单位：秒）
     */
    public void setMute(String uid, int time) {
        RedisValue<Object> value = RedisValue.of(template, SocketTree.MUTE.getPath(uid));
        value.set((int) (System.currentTimeMillis() / 1000), time);
    }

    /**
     * 临时限制登录
     *
     * @param uid  目标用户
     * @param time 时间（单位：秒）
     */
    public void setLock(String uid, int time) {
        RedisValue<Object> value = RedisValue.of(template, SocketTree.LOCK.getPath(uid));
        value.set((int) (System.currentTimeMillis() / 1000), time);
    }

    /**
     * 获取禁言剩余时间（单位：秒）
     */
    public long getMuteTime(String uid) {
        RedisValue<Object> value = RedisValue.of(template, SocketTree.MUTE.getPath(uid));
        return value.getExpired();
    }

    /**
     * 获取冻结剩余时间（单位：秒）
     */
    public long getLockTime(String uid) {
        RedisValue<Object> value = RedisValue.of(template, SocketTree.LOCK.getPath(uid));
        return value.getExpired();
    }

    /**
     * 发言次数标记
     *
     * @param uid 要标记的uid
     * @return 发言次数
     */
    public long incrSpeak(String uid) {
        RedisValue<Object> value = RedisValue.of(template, SocketTree.SPEAK.getPath(uid));
        return value.exist() ? value.incr() : value.incr(1, 10);
    }

    /**
     * 设置未读记录数
     *
     * @param uid    未读用户信息
     * @param target 未读目标用户
     * @param delta  递增/递减阈值（0清除未读消息）
     */
    public void setUnreadCount(String uid, String target, int delta) {
        RedisMap<String, Object> map = RedisValue.ofMap(template, RedisTree.UNREAD.getPath(uid));
        if (delta == 0 || map.increment(target, delta) == 0) {
            map.remove(target);
        }
    }

    /**
     * 获取指定用户和目标的未读消息数量
     *
     * @param uid    发起者
     * @param target 目标用户
     * @return 未读消息数量
     */
    public int getUnreadCount(String uid, String target) {
        Map<String, Object> map = RedisValue.ofMap(template, RedisTree.UNREAD.getPath(uid));
        return (int) map.getOrDefault(target, 0);
    }

    /**
     * 设置公告内容
     *
     * @param content 公告
     */
    public void pushNotice(String content) {
        RedisMap<String, Object> map = RedisValue.ofMap(template, RedisTree.ANNOUNCEMENT.getPath());
        // 公告为空删除
        if (content.isEmpty()) {
            map.clear();
            return;
        }
        map.put(Announce.content.string(), content);
        map.put(Announce.digest.string(), MD5.create().digestHex(content));
        map.put(Announce.time.string(), System.currentTimeMillis());
    }
}
