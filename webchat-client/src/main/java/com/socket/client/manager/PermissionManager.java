package com.socket.client.manager;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.model.enums.OnlineState;
import com.socket.client.support.KeywordSupport;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.mapper.ShieldUserMapper;
import com.socket.webchat.mapper.SysUserLogMapper;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.*;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ws权限管理器
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PermissionManager {
    private final KeywordSupport keywordSupport;

    private final SysUserLogMapper sysUserLogMapper;
    private final ShieldUserMapper shieldUserMapper;
    private final SysUserMapper sysUserMapper;
    private final RecordService recordService;

    private final RedisManager redisManager;
    private final GroupManager groupManager;
    private final UserManager userManager;


    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public Collection<UserPreview> getUserPreviews(WsUser self) {
        // 消息发起者
        String suid = self.getUid();
        // 与此用户关联的所有未读消息
        Collection<ChatRecord> unreadMessages = recordService.getLatestUnreadMessages(suid);
        // 登录记录
        Map<String, Date> logs = this.getUserLoginLogs();
        // 链接数据
        List<UserPreview> previews = new ArrayList<>();
        for (WsUser user : userManager.values()) {
            UserPreview preview = new UserPreview(user);
            // 最后登录时间
            Date time = logs.get(preview.getUid());
            if (time != null) {
                preview.setLastTime(time.getTime());
            }
            // 检查未读消息
            String target = preview.getUid();
            int count = redisManager.getUnreadCount(suid, target);
            if (count > 0) {
                unreadMessages.stream()
                        .filter(e -> e.getUid().equals(target))
                        .findFirst()
                        .ifPresent(unread -> {
                            MessageType type = unread.getType();
                            preview.setPreview(type == MessageType.TEXT ? unread.getContent() : '[' + type.getPreview() + ']');
                            preview.setLastTime(unread.getCreateTime().getTime());
                            preview.setUnreads(Math.min(count, 99));
                        });
            }
            // 为自己赋值屏蔽列表
            if (preview.getUid().equals(suid)) {
                preview.setShields(getShield(self));
            }
            previews.add(preview);
        }
        // 添加群组到列表
        for (Map.Entry<SysGroup, List<WsUser>> entry : groupManager.entrySet()) {
            SysGroup group = entry.getKey();
            List<String> uids = entry.getValue().stream().map(SysUser::getUid).collect(Collectors.toList());
            // 需要在群里
            if (uids.contains(suid)) {
                UserPreview preview = new UserPreview();
                preview.setIsgroup(true);
                preview.setMembers(uids);
                preview.setUid(group.getGroupId());
                preview.setName(group.getName());
                preview.setOwner(group.getOwner());
                preview.setOnline(OnlineState.ONLINE);
                previews.add(preview);
            }
        }
        return previews;
    }

    /**
     * 获取所有用户登录最新时间
     */
    private Map<String, Date> getUserLoginLogs() {
        QueryWrapper<SysUserLog> wrapper = Wrappers.query();
        wrapper.select(Wss.columnToString(SysUserLog::getUid), Wss.selecterMax(BaseModel::getCreateTime));
        wrapper.lambda().groupBy(SysUserLog::getUid);
        List<SysUserLog> userLogs = sysUserLogMapper.selectList(wrapper);
        return userLogs.stream().collect(Collectors.toMap(SysUserLog::getUid, BaseModel::getCreateTime));
    }

    /**
     * 设置指定用户头衔
     *
     * @param target 目标用户
     * @param alias  头衔
     * @return 是否成功
     */
    public boolean updateAlias(WsUser target, String alias) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getUid, target.getUid());
        wrapper.set(SysUser::getAlias, alias);
        if (sysUserMapper.update(null, wrapper) == 1) {
            target.setAlias(alias);
            return true;
        }
        return false;
    }

    /**
     * 检查指定用户禁言情况，若用户被禁言将发送一条系统通知
     */
    public void checkMute(WsUser user) {
        long time = redisManager.getMuteTime(user.getUid());
        if (time > 0) {
            user.send(Callback.MUTE_LIMIT.format(time), MessageType.MUTE, time);
        }
    }

    /**
     * 推送公告
     *
     * @param wsmsg 系统消息
     */
    public void pushNotice(WsMsg wsmsg, SysUser sender) {
        String content = wsmsg.getContent();
        redisManager.pushNotice(content);
        if (StrUtil.isNotEmpty(content)) {
            userManager.sendAll(content, MessageType.ANNOUNCE, sender);
        }
    }

    /**
     * 检查消息合法性
     *
     * @param wsuser    发起者
     * @param wsmsg     消息
     * @param sensitive 敏感关键词检查
     * @return 是否通过
     */
    public boolean verifyMessage(WsUser wsuser, WsMsg wsmsg, boolean sensitive) {
        String content = wsmsg.getContent();
        if (content == null) {
            return true;
        }
        if (ReUtil.isMatch("</?\\w+(\\s.+?)?>", content)) {
            wsuser.reject(Callback.VIOLATION_CHARACTER, wsmsg);
            return false;
        }
        content = StrUtil.sub(content, 0, Constants.MAX_MESSAGE_LENGTH);
        if (sensitive && keywordSupport.containsSensitive(content)) {
            wsuser.reject(Callback.SENSITIVE_KEYWORDS, wsmsg);
            return false;
        }
        wsmsg.setContent(content);
        return true;
    }


    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(WsUser secure, WsUser target) {
        return getShield(secure).contains(target.getUid());
    }

    /**
     * 获取指定用户屏蔽列表
     *
     * @param wsuser 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(WsUser wsuser) {
        return redisManager.getShield(wsuser.getUid());
    }

    /**
     * 屏蔽/取消屏蔽 指定用户
     *
     * @param user   用户信息
     * @param target 目标用户
     * @return 若成功屏蔽返回true, 取消屏蔽返回false
     */
    public boolean shieldTarget(WsUser user, WsUser target) {
        String uid = user.getUid();
        List<String> shields = this.getShield(user);
        String tuid = target.getUid();
        LambdaUpdateWrapper<ShieldUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ShieldUser::getUid, uid);
        wrapper.eq(ShieldUser::getTarget, tuid);
        // 包含此目标uid，取消屏蔽
        if (shields.contains(tuid)) {
            wrapper.set(ShieldUser::isDeleted, 1);
            shieldUserMapper.update(null, wrapper);
            shields.remove(tuid);
            return false;
        }
        // 不包含目标uid，屏蔽
        wrapper.set(ShieldUser::isDeleted, 0);
        // 更新失败则添加
        if (shieldUserMapper.update(null, wrapper) == 0) {
            ShieldUser suser = new ShieldUser();
            suser.setUid(uid);
            suser.setTarget(tuid);
            shieldUserMapper.insert(suser);
        }
        return shields.add(tuid);
    }

    /**
     * 连续发言标记（排除所有者） <br>
     * 10秒内超过一定次数会被禁止一段时间发言
     */
    public void operateMark(WsUser user) {
        if (!user.isOwner()) {
            long time = TimeUnit.HOURS.toSeconds(Constants.FREQUENT_SPEECHES_MUTE_TIME);
            if (redisManager.incrSpeak(user.getUid()) > Constants.FREQUENT_SPEECH_THRESHOLD) {
                redisManager.setMute(user.getUid(), time);
                user.send(Callback.BRUSH_SCREEN.format(time), MessageType.MUTE, time);
            }
        }
    }

    /**
     * 更新用户权限
     *
     * @param user 指定用户
     * @param role 新权限
     */
    public void updateRole(WsUser user, UserRole role) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getUid, user.getUid());
        wrapper.set(SysUser::getRole, role.getRole());
        sysUserMapper.update(null, wrapper);
        user.setRole(role);
    }

    /**
     * 检查指定用户是否被禁言
     *
     * @param user 用户
     * @return true被禁言
     */
    public boolean isMute(WsUser user) {
        return redisManager.getMuteTime(user.getUid()) > 0;
    }

    /**
     * 禁言指定用户
     *
     * @return 禁言时间
     */
    public long addMute(WsMsg wsmsg) {
        int time = Integer.parseInt(wsmsg.getContent());
        redisManager.setMute(wsmsg.getTarget(), time);
        return time;
    }

    /**
     * 冻结指定用户
     *
     * @return 冻结时间
     */
    public long addLock(WsMsg wsmsg) {
        int time = Integer.parseInt(wsmsg.getContent());
        redisManager.setLock(wsmsg.getTarget(), time);
        return time;
    }

    /**
     * 获取消息发送的目标（用户/群组）
     *
     * @param wsmsg 消息
     * @return 目标
     */
    public WsUser getTarget(WsMsg wsmsg) {
        String target = wsmsg.getTarget();
        if (wsmsg.isGroup()) {
            WsUser user = new WsUser();
            SysGroup group = groupManager.getGroup(target);
            user.setUid(group.getGroupId());
            user.setName(group.getName());
            return user;
        }
        return userManager.getUser(target);
    }
}
