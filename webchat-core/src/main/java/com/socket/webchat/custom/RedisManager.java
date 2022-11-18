package com.socket.webchat.custom;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.mapper.ShieldUserMapper;
import com.socket.webchat.model.Announce;
import com.socket.webchat.model.ShieldUser;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ws redis管理器
 */
@Component
@RequiredArgsConstructor
public class RedisManager {
    private final ShieldUserMapper shieldUserMapper;
    private final RedisClient<Integer> permission;
    private final RedisClient<Object> announce;
    private final RedisClient<String> shield;

    /**
     * 临时禁言
     *
     * @param uid  目标用户
     * @param time 时间（单位：秒）
     */
    public void setMute(String uid, long time) {
        long value = (System.currentTimeMillis() / 1000) + time;
        permission.set(RedisTree.MUTE.concat(uid), (int) value, time);
    }

    /**
     * 临时限制登录
     *
     * @param uid  目标用户
     * @param time 时间（单位：秒）
     */
    public void setLock(String uid, long time) {
        long value = (System.currentTimeMillis() / 1000) + time;
        permission.set(RedisTree.LOCK.concat(uid), (int) value, time);
    }

    /**
     * 获取禁言剩余时间（单位：秒）
     */
    public long getMuteTime(String uid) {
        return permission.getExpired(RedisTree.MUTE.concat(uid));
    }

    /**
     * 获取冻结剩余时间（单位：秒）
     */
    public long getLockTime(String uid) {
        return permission.getExpired(RedisTree.LOCK.concat(uid));
    }

    /**
     * 发言次数标记
     *
     * @param uid 要标记的uid
     * @return 发言次数
     */
    public long incrSpeak(String uid) {
        String key = RedisTree.SPEAK.concat(uid);
        return permission.exist(key) ? permission.incr(key, 1) : permission.incr(key, 1, 10);
    }

    /**
     * 设置未读记录数
     *
     * @param uid    未读用户信息
     * @param target 未读目标用户
     * @param delta  递增/递减阈值（0清除未读消息）
     */
    public void setUnreadCount(String uid, String target, int delta) {
        RedisMap<String, Integer> map = permission.withMap(RedisTree.UNREAD.concat(uid));
        if (delta == 0 || map.increment(target, delta) <= 0) {
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
        Map<String, Integer> map = permission.withMap(RedisTree.UNREAD.concat(uid));
        return map.getOrDefault(target, 0);
    }

    /**
     * 设置公告内容
     *
     * @param content 公告
     */
    public void pushNotice(String content) {
        RedisMap<String, Object> map = announce.withMap(RedisTree.ANNOUNCE.get());
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
     * @param uid 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(String uid) {
        RedisList<String> redisList = shield.withList(RedisTree.SHIELD.concat(uid));
        // 检查缓存
        if (redisList.isEmpty()) {
            // 查询数据库
            LambdaQueryWrapper<ShieldUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(ShieldUser::getUid, uid);
            List<ShieldUser> users = shieldUserMapper.selectList(wrapper);
            List<String> collect = users.stream().map(ShieldUser::getTarget).collect(Collectors.toList());
            redisList.addAll(collect);
            redisList.expire(Constants.SHIELD_CACHE_TIME, TimeUnit.HOURS);
        }
        return redisList;
    }
}