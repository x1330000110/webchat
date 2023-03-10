package com.socket.core.custom;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.core.constant.ChatProperties;
import com.socket.core.mapper.ShieldUserMapper;
import com.socket.core.model.Announce;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.model.po.ShieldUser;
import com.socket.core.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Socket Redis管理器
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class SocketRedisManager {
    private final ShieldUserMapper shieldUserMapper;
    private final ChatProperties properties;
    private final RedisClient client;

    /**
     * 临时禁言
     *
     * @param guid 目标用户
     * @param time 时间（单位：秒）
     */
    public void setMute(String guid, long time) {
        long value = (System.currentTimeMillis() / 1000) + time;
        client.set(RedisTree.MUTE.concat(guid), (int) value, time);
    }

    /**
     * 临时限制登录
     *
     * @param guid 目标用户
     * @param time 时间（单位：秒）
     */
    public void setLock(String guid, long time) {
        long value = (System.currentTimeMillis() / 1000) + time;
        client.set(RedisTree.LOCK.concat(guid), (int) value, time);
    }

    /**
     * 获取禁言剩余时间（单位：秒）
     */
    public long getMuteTime(String guid) {
        return client.getExpired(RedisTree.MUTE.concat(guid));
    }

    /**
     * 获取冻结剩余时间（单位：秒）
     */
    public long getLockTime(String guid) {
        return client.getExpired(RedisTree.LOCK.concat(guid));
    }

    /**
     * 发言次数标记
     *
     * @param guid 要标记的uid
     * @return 发言次数
     */
    public long incrSpeak(String guid) {
        String key = RedisTree.SPEAK.concat(guid);
        return client.exist(key) ? client.incr(key, 1) : client.incr(key, 1, 10);
    }

    /**
     * 设置未读记录数
     *
     * @param guid   未读用户信息
     * @param target 未读目标用户
     * @param delta  递增/递减阈值（0清除未读消息）
     */
    public void setUnreadCount(String guid, String target, int delta) {
        RedisMap<String, Integer> map = client.withMap(RedisTree.UNREAD.concat(guid));
        if (delta == 0 || map.increment(target, delta) <= 0) {
            map.remove(target);
        }
    }

    /**
     * 获取指定用户和目标的未读消息数量
     *
     * @param guid   发起者
     * @param target 目标用户
     * @return 未读消息数量
     */
    public int getUnreadCount(String guid, String target) {
        Map<String, Integer> map = client.withMap(RedisTree.UNREAD.concat(guid));
        return map.getOrDefault(target, 0);
    }

    /**
     * 设置公告内容
     *
     * @param content 公告
     */
    public void pushNotice(String content) {
        RedisMap<String, Object> map = client.withMap(RedisTree.ANNOUNCE.get());
        // 公告为空删除
        if (content.isEmpty()) {
            map.clear();
            return;
        }
        map.putAll(BeanUtil.beanToMap(new Announce(content)));
    }

    /**
     * 获取指定用户的屏蔽列表
     *
     * @param guid 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(String guid) {
        RedisList<String> redisList = client.withList(RedisTree.SHIELD.concat(guid));
        // 检查缓存
        if (redisList.isEmpty()) {
            // 查询数据库
            LambdaQueryWrapper<ShieldUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(ShieldUser::getGuid, guid);
            List<ShieldUser> users = shieldUserMapper.selectList(wrapper);
            List<String> collect = users.stream().map(ShieldUser::getTarget).collect(Collectors.toList());
            redisList.addAll(collect);
            redisList.expire(properties.getShieldCacheTime(), TimeUnit.HOURS);
        }
        return redisList;
    }
}
