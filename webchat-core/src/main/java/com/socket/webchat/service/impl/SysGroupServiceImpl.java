package com.socket.webchat.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.service.SysGroupService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysGroupServiceImpl extends ServiceImpl<SysGroupMapper, SysGroup> implements SysGroupService {
    private final SysGroupUserMapper sysGroupUserMapper;

    public boolean joinGroup(String groupId, String uid) {
        LambdaQueryWrapper<SysGroupUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysGroupUser::getUid, uid);
        if (sysGroupUserMapper.selectOne(wrapper) != null) {
            return false;
        }
        SysGroupUser user = new SysGroupUser();
        user.setGroupId(groupId);
        user.setUid(uid);
        return SqlHelper.retBool(sysGroupUserMapper.insert(user));
    }

    public void createGroup(String groupName) {
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
        super.save(group);
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
        return SqlHelper.retBool(sysGroupUserMapper.update(null, wrapper2));
    }
}
