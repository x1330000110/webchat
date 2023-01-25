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
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Bcrypt;
import com.socket.webchat.util.Publisher;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysGroupServiceImpl extends ServiceImpl<SysGroupMapper, SysGroup> implements SysGroupService {
    private final SysGroupUserMapper sysGroupUserMapper;
    private final Publisher publisher;

    public String createGroup(String groupName, String password) {
        String userId = Wss.getUserId();
        // 创建检查
        boolean expression = StrUtil.isEmpty(password) || password.length() <= Constants.MAX_GROUP_PASSWORD;
        Assert.isTrue(expression, "密码长度不合法", IllegalStateException::new);
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
        String gid = Constants.GROUP_PREFIX + RandomUtil.randomNumbers(6);
        group.setGuid(gid);
        group.setName(groupName);
        group.setOwner(userId);
        // 散列密码
        if (StrUtil.isNotEmpty(password)) {
            group.setPassword(Bcrypt.digest(password));
        }
        if (super.save(group)) {
            // 推送事件
            publisher.pushGroupEvent(group, GroupEnum.CREATE);
            // 加入新建的群组里
            joinGroup(gid, userId);
            return gid;
        }
        return null;
    }

    @Override
    public List<String> joinGroup(String gid, String uid, String password) {
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BaseUser::getGuid, gid);
        SysGroup group = getFirst(wrapper);
        Assert.notNull(group, "找不到群组", IllegalStateException::new);
        String hash = group.getPassword();
        if (StrUtil.isNotEmpty(hash)) {
            Assert.isTrue(Bcrypt.verify(password, hash), "入群密码不正确", IllegalStateException::new);
        }
        return joinGroup(gid, uid);
    }

    public List<String> joinGroup(String gid, String uid) {
        LambdaQueryWrapper<SysGroupUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroupUser::getGid, gid);
        List<String> guids = sysGroupUserMapper.selectList(wrapper)
                .stream()
                .map(SysGroupUser::getUid)
                .collect(Collectors.toList());
        Assert.isFalse(guids.contains(uid), "您已经是该群组成员", IllegalStateException::new);
        SysGroupUser user = new SysGroupUser(gid, uid);
        // 推送事件
        if (SqlHelper.retBool(sysGroupUserMapper.insert(user))) {
            publisher.pushGroupEvent(user, GroupEnum.JOIN);
            return guids;
        }
        return null;
    }

    public boolean removeUser(String gid, String uid) {
        String stater = Wss.getUserId();
        LambdaQueryWrapper<SysGroup> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroup::getGuid, gid);
        SysGroup group = getFirst(wrapper);
        // 权限检查
        Assert.isTrue(group.getOwner().equals(stater), "你不是此群的创建者", IllegalStateException::new);
        LambdaUpdateWrapper<SysGroupUser> wrapper2 = Wrappers.lambdaUpdate();
        wrapper2.eq(SysGroupUser::getUid, uid);
        wrapper2.set(BaseModel::isDeleted, 1);
        // 推送事件
        if (SqlHelper.retBool(sysGroupUserMapper.update(null, wrapper2))) {
            SysGroupUser groupUser = new SysGroupUser(gid, uid);
            publisher.pushGroupEvent(groupUser, GroupEnum.DELETE);
            return true;
        }
        return false;
    }

    public boolean dissolveGroup(String gid) {
        String userId = Wss.getUserId();
        LambdaUpdateWrapper<SysGroup> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysGroup::getGuid, gid);
        wrapper.eq(SysGroup::getOwner, userId);
        wrapper.set(BaseModel::isDeleted, 1);
        if (super.update(wrapper)) {
            // 推送事件
            SysGroup group = new SysGroup();
            group.setGuid(gid);
            group.setOwner(userId);
            publisher.pushGroupEvent(group, GroupEnum.DISSOLVE);
            return true;
        }
        return false;
    }

    @Override
    public boolean exitGroup(String gid) {
        String userId = Wss.getUserId();
        LambdaUpdateWrapper<SysGroupUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysGroupUser::getGid, gid);
        wrapper.eq(SysGroupUser::getUid, userId);
        wrapper.set(BaseModel::isDeleted, 1);
        int ok = sysGroupUserMapper.update(null, wrapper);
        if (SqlHelper.retBool(ok)) {
            // 推送事件
            SysGroupUser user = new SysGroupUser(gid, userId);
            publisher.pushGroupEvent(user, GroupEnum.EXIT);
            return true;
        }
        return false;
    }
}
