package com.socket.core.custom;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.socket.core.model.AuthUser;
import com.socket.core.model.enums.RedisTree;
import com.socket.core.util.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Token用户与缓存
 */
@Component
@RequiredArgsConstructor
public class TokenUserManager {
    private final RedisClient<Object> client;

    /**
     * 设置Token到缓存
     *
     * @param uid     用户id
     * @param key     加密密钥
     * @param expired 过期时间（单位：秒）
     * @return token
     */
    public String setToken(String uid, String key, long expired) {
        String token = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER.substring(0, 16), 16);
        AuthUser user = new AuthUser(uid, key);
        client.set(RedisTree.AUTH.concat(token), BeanUtil.beanToMap(user), expired);
        return token;
    }

    /**
     * 更新Token加密密钥
     *
     * @param token 令牌
     * @param key   密钥
     */
    public void setEncKey(String token, String key) {
        AuthUser user = getTokenUser(token);
        if (user != null) {
            user.setKey(key);
            client.setIfPresent(RedisTree.AUTH.concat(token), BeanUtil.beanToMap(user));
        }
    }

    /**
     * 获取Token关联的用户
     *
     * @param token 令牌
     * @return {@link AuthUser}
     */
    public AuthUser getTokenUser(String token) {
        Object obj = client.get(RedisTree.AUTH.concat(token));
        return obj == null ? null : BeanUtil.toBean(obj, AuthUser.class);
    }

    /**
     * 移除Token关联的用户
     *
     * @param token 令牌
     */
    public void removeUser(String token) {
        client.remove(RedisTree.AUTH.concat(token));
    }
}
