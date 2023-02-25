package com.socket.client.manager;

import cn.hutool.core.util.StrUtil;
import com.socket.client.custom.KeywordSupport;
import com.socket.core.constant.Constants;
import com.socket.core.custom.RedisManager;
import com.socket.core.mapper.SysGroupMapper;
import com.socket.core.mapper.SysGroupUserMapper;
import com.socket.core.mapper.SysUserMapper;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.po.*;
import com.socket.core.model.ws.GroupPreview;
import com.socket.core.model.ws.UserPreview;
import com.socket.core.model.ws.WsMsg;
import com.socket.core.model.ws.WsUser;
import com.socket.core.service.ChatRecordService;
import com.socket.core.service.SysUserLogService;
import com.socket.core.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ws权限管理器
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PermissionManager implements InitializingBean {
    private final KeywordSupport keywordSupport;

    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysUserLogService sysUserLogService;
    private final SysGroupMapper sysGroupMapper;
    private final ChatRecordService chatRecordService;
    private final SysUserMapper sysUserMapper;

    private final RedisManager redisManager;
    private final SocketGroupMap groupMap;
    private final SocketUserMap userMap;


    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public List<BaseUser> getUserPreviews(WsUser self) {
        // 消息发起者
        String suid = self.getGuid();
        // 与此用户关联的最新未读消息
        Map<String, ChatRecord> unreadMessages = chatRecordService.getLatestUnreadMessages(suid);
        // 登录记录
        Map<String, SysUserLog> logs = sysUserLogService.getLatestUserLogs();
        // 链接数据
        List<BaseUser> previews = new ArrayList<>();
        for (WsUser user : userMap.values()) {
            UserPreview preview = new UserPreview(user);
            String target = preview.getGuid();
            // 关联日志
            SysUserLog log = logs.get(target);
            if (log != null) {
                preview.setLastTime(log.getCreateTime().getTime());
                preview.setProvince(log.getProvince());
            }
            // 检查未读消息
            int count = redisManager.getUnreadCount(suid, target);
            if (count > 0) {
                ChatRecord unread = unreadMessages.get(target);
                if (unread != null) {
                    preview.setPreview(unread);
                    preview.setLastTime(unread.getCreateTime().getTime());
                    preview.setUnreads(Math.min(count, 99));
                }
            }
            // 为自己赋值屏蔽列表
            if (target.equals(suid)) {
                preview.setShields(getShield(self));
            }
            previews.add(preview);
        }
        // 添加群组到列表
        groupMap.forEach((group, value) -> {
            List<String> uids = value.stream().map(SysUser::getGuid).collect(Collectors.toList());
            // 需要在群里
            if (uids.contains(suid)) {
                GroupPreview preview = new GroupPreview(group);
                preview.setIsgroup(true);
                preview.setGuids(uids);
                previews.add(preview);
            }
        });
        return previews;
    }

    /**
     * 获取指定用户屏蔽列表
     *
     * @param wsuser 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(WsUser wsuser) {
        return redisManager.getShield(wsuser.getGuid());
    }

    /**
     * 检查指定用户禁言情况，若用户被禁言将发送一条系统通知
     */
    public void checkMute(WsUser user) {
        long time = redisManager.getMuteTime(user.getGuid());
        if (time > 0) {
            user.send(String.valueOf(time), PermissionEnum.MUTE, user);
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
        content = StrUtil.sub(content, 0, Constants.MAX_MESSAGE_LENGTH);
        // 违规字符检查的代替方案
        content = content.replace("<", "&lt;");
        content = content.replace(">", "&gt;");
        if (sensitive && keywordSupport.containsSensitive(content)) {
            wsuser.reject("消息包含敏感关键词，请检查后重新发送", wsmsg);
            return false;
        }
        wsmsg.setContent(content);
        return true;
    }

    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(WsUser source, WsUser target) {
        return getShield(source).contains(target.getGuid());
    }

    /**
     * 连续发言标记（排除所有者） <br>
     * 10秒内超过一定次数会被禁止一段时间发言
     */
    public void operateMark(WsUser user) {
        if (!user.isOwner()) {
            long time = TimeUnit.HOURS.toSeconds(Constants.FREQUENT_SPEECHES_MUTE_TIME);
            if (redisManager.incrSpeak(user.getGuid()) > Constants.FREQUENT_SPEECH_THRESHOLD) {
                redisManager.setMute(user.getGuid(), time);
                // b结尾 特殊的刷屏标记
                user.send(time + "b", PermissionEnum.MUTE, user);
            }
        }
    }

    /**
     * 检查指定用户是否被禁言
     *
     * @param user 用户
     * @return true被禁言
     */
    public boolean isMute(WsUser user) {
        return redisManager.getMuteTime(user.getGuid()) > 0;
    }

    /**
     * 检查用户/群组是否存在
     *
     * @param target 目标uid
     * @return 是否存在
     */
    public boolean notHas(String target) {
        if (Wss.isGroup(target)) {
            return groupMap.get(target) == null;
        }
        return userMap.get(target) == null;
    }

    @Override
    public void afterPropertiesSet() {
        // 缓存用户
        List<SysUser> userList = sysUserMapper.selectList(null);
        userList.stream().map(WsUser::new).forEach(e -> userMap.put(e.getGuid(), e));
        // 缓存群组
        List<SysGroup> sysGroups = sysGroupMapper.selectList(null);
        List<SysGroupUser> groupUsers = sysGroupUserMapper.selectList(null);
        for (SysGroup group : sysGroups) {
            List<WsUser> collect = groupUsers.stream()
                    .filter(e -> e.getGid().equals(group.getGuid()))
                    .map(SysGroupUser::getUid)
                    .map(userMap::get)
                    .collect(Collectors.toList());
            groupMap.put(group, collect);
        }
    }
}
