package com.socket.webchat.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.service.SysGroupService;
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

    public String createGroup(String groupName) {
        String userId = Wss.getUserId();
        // 创建检查
        LambdaQueryWrapper<SysGroup> check = Wrappers.lambdaQuery();
        check.eq(SysGroup::getOwner, userId);
        int count = count(check);
        Assert.isTrue(count <= Constants.MAX_CREATE_GROUP_NUM, "群组创建已达上限", IllegalStateException::new);
        // 必要的组名检查
        Assert.isFalse(StrUtil.isEmpty(groupName), "空的群组名称", IllegalStateException::new);
        Assert.isTrue(StrUtil.length(groupName) <= 8, "无效的群组名称", IllegalStateException::new);
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroup::getName, groupName);
        Assert.isNull(getFirst(wrapper), "群组名称已存在", IllegalStateException::new);
        // 写入数据库
        SysGroup group = new SysGroup();
        String groupId = Constants.GROUP + RandomUtil.randomNumbers(6);
        group.setGroupId(groupId);
        group.setName(groupName);
        group.setOwner(userId);
        if (super.save(group)) {
            // 推送事件
            publisher.publishEvent(new GroupChangeEvent(publisher, group, GroupEnum.CREATE));
            // 加入新建的群组里
            joinGroup(groupId, userId);
            return groupId;
        }
        return null;
    }

    public boolean joinGroup(String groupId, String uid) {
        LambdaQueryWrapper<SysGroupUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroupUser::getGroupId, groupId);
        wrapper.eq(SysGroupUser::getUid, uid);
        if (sysGroupUserMapper.selectOne(wrapper) != null) {
            return false;
        }
        SysGroupUser user = new SysGroupUser(groupId, uid);
        // 推送事件
        if (SqlHelper.retBool(sysGroupUserMapper.insert(user))) {
            publisher.publishEvent(new GroupChangeEvent(publisher, user, GroupEnum.JOIN));
            return true;
        }
        return false;
    }

    public boolean removeUser(String stater, String groupId, String uid) {
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroup::getGroupId, groupId);
        SysGroup group = getFirst(wrapper);
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
            publisher.publishEvent(new GroupChangeEvent(publisher, sysGroupUser, GroupEnum.DELETE));
            return true;
        }
        return false;
    }

    public boolean dissolveGroup(String groupId) {
        String userId = Wss.getUserId();
        LambdaUpdateWrapper<SysGroup> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysGroup::getGroupId, groupId);
        wrapper.eq(SysGroup::getOwner, userId);
        wrapper.set(BaseModel::isDeleted, 1);
        if (super.update(wrapper)) {
            // 推送事件
            SysGroup group = new SysGroup();
            group.setGroupId(groupId);
            group.setOwner(userId);
            publisher.publishEvent(new GroupChangeEvent(publisher, group, GroupEnum.DISSOLVE));
            return true;
        }
        return false;
    }

    @Override
    public boolean exitGroup(String groupId) {
        String userId = Wss.getUserId();
        LambdaUpdateWrapper<SysGroupUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysGroupUser::getGroupId, groupId);
        wrapper.eq(SysGroupUser::getUid, userId);
        wrapper.set(BaseModel::isDeleted, 1);
        int ok = sysGroupUserMapper.update(null, wrapper);
        if (SqlHelper.retBool(ok)) {
            // 推送事件
            SysGroupUser user = new SysGroupUser(groupId, userId);
            publisher.publishEvent(new GroupChangeEvent(publisher, user, GroupEnum.EXIT));
            return true;
        }
        return false;
    }
}
