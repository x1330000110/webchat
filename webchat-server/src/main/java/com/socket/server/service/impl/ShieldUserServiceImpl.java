package com.socket.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.core.custom.SocketRedisManager;
import com.socket.core.mapper.ShieldUserMapper;
import com.socket.core.model.po.ShieldUser;
import com.socket.server.service.ShieldUserService;
import com.socket.server.util.ShiroUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShieldUserServiceImpl extends ServiceImpl<ShieldUserMapper, ShieldUser> implements ShieldUserService {
    private final SocketRedisManager redisManager;

    public boolean shieldTarget(String target) {
        String guid = ShiroUser.getUserId();
        List<String> shields = redisManager.getShield(guid);
        LambdaUpdateWrapper<ShieldUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ShieldUser::getGuid, guid);
        wrapper.eq(ShieldUser::getTarget, target);
        // 包含此目标uid，取消屏蔽
        if (shields.contains(target)) {
            wrapper.set(ShieldUser::isDeleted, 1);
            update(wrapper);
            shields.remove(target);
            return false;
        }
        // 不包含目标uid，屏蔽
        wrapper.set(ShieldUser::isDeleted, 0);
        // 更新失败则添加
        if (update(wrapper)) {
            ShieldUser suser = new ShieldUser();
            suser.setGuid(guid);
            suser.setTarget(target);
            save(suser);
        }
        return shields.add(target);
    }
}
