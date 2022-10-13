package com.socket.webchat.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.listener.GroupChangeEvent;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.enums.GroupOperation;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysGroupServiceImpl extends ServiceImpl<SysGroupMapper, SysGroup> implements SysGroupService {
    private final SysGroupUserMapper sysGroupUserMapper;
    private final ApplicationEventPublisher publisher;

    public boolean joinGroup(String groupId, String uid) {
        LambdaQueryWrapper<SysGroupUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroupUser::getUid, uid);
        if (sysGroupUserMapper.selectOne(wrapper) != null) {
            return false;
        }
        SysGroupUser user = new SysGroupUser();
        user.setGroupId(groupId);
        user.setUid(uid);
        // 推送事件
        if (SqlHelper.retBool(sysGroupUserMapper.insert(user))) {
            GroupChangeEvent event = new GroupChangeEvent(publisher, user, GroupOperation.JOIN);
            publisher.publishEvent(event);
            return true;
        }
        return false;
    }

    public boolean removeUser(String stater, String groupId, String uid) {
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroup::getGroupId, groupId);
        SysGroup group = getOne(wrapper);
        // 权限检查
        if (!group.getOwner().equals(stater)) {
            return false;
        }
        LambdaUpdateWrapper<SysGroupUser> wrapper2 = Wrappers.lambdaUpdate();
        wrapper2.eq(SysGroupUser::getUid, uid);
        wrapper2.set(BaseModel::isDeleted, 1);
        // 推送事件
        if (SqlHelper.retBool(sysGroupUserMapper.update(null, wrapper2))) {
            SysGroupUser sysGroupUser = new SysGroupUser(groupId, uid);
            GroupChangeEvent event = new GroupChangeEvent(publisher, sysGroupUser, GroupOperation.DELETE);
            publisher.publishEvent(event);
            return true;
        }
        return false;
    }

    public boolean createGroup(String groupName) {
        // 必要的组名检查
        Assert.isFalse(StrUtil.isEmpty(groupName), "空的群组名称", IllegalStateException::new);
        Assert.isTrue(StrUtil.length(groupName) <= 8, "无效的群组名称", IllegalStateException::new);
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroup::getName, groupName);
        Assert.isNull(getOne(wrapper), "群组名称已存在", IllegalStateException::new);
        // 写入数据库
        SysGroup group = new SysGroup();
        group.setGroupId(Constants.GROUP + RandomUtil.randomNumbers(6));
        group.setName(groupName);
        group.setOwner(Wss.getUserId());
        if (super.save(group)) {
            // 推送事件
            GroupChangeEvent event = new GroupChangeEvent(publisher, group, GroupOperation.CREATE);
            publisher.publishEvent(event);
            return true;
        }
        return false;
    }

    public boolean removeGroup(String owner, String groupId) {
        LambdaUpdateWrapper<SysGroup> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysGroup::getGroupId, groupId);
        wrapper.eq(SysGroup::getOwner, owner);
        if (super.update(wrapper)) {
            // 推送事件
            GroupChangeEvent event = new GroupChangeEvent(publisher, new SysGroup(), GroupOperation.DISSOLUTION);
            publisher.publishEvent(event);
            return true;
        }
        return false;
    }
}
