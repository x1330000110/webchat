package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.model.enums.Remote;
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
import org.apache.shiro.subject.Subject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 在线用户信息管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocketManager {
    private final ConcurrentHashMap<String, WsUser> onlines = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SysUserLogMapper sysUserLogMapper;
    private final ShieldUserMapper shieldUserMapper;
    private final RecordService recordService;
    private final SysUserMapper sysUserMapper;
    private final RedisManager redisManager;

    /**
     * 加入用户
     *
     * @param session    ws session
     * @param properties 配置信息
     * @return 已加入的用户对象
     */
    public WsUser join(Session session, Map<String, Object> properties) {
        // 构建聊天室用户
        Subject subject = (Subject) properties.get(Constants.SUBJECT);
        HttpSession httpSession = (HttpSession) properties.get(Constants.HTTP_SESSION);
        String platform = (String) properties.get(Constants.PLATFORM);
        WsUser user = new WsUser(session, subject, httpSession, platform);
        // 检查登录限制（会话缓存检查）
        long time = redisManager.getLockTime(user.getUid());
        if (time > 0) {
            user.logout(Callback.LOGIN_LIMIT, time);
            return null;
        }
        // 检查重复登录
        WsUser repeat = onlines.get(user.getUid());
        if (repeat != null) {
            repeat.logout(Callback.REPEAT_LOGIN);
        }
        // 登录到聊天室
        onlines.put(user.getUid(), user);
        return user;
    }

    /**
     * 向所有用户发送消息（不包括发起者和屏蔽的用户）<br>
     *
     * @param wsmsg  消息
     * @param sender 发起者
     */
    public void sendAll(WsMsg wsmsg, WsUser sender) {
        List<String> exclude = redisManager.getShield(sender.getUid());
        String uid = sender.getUid();
        for (WsUser wsuser : onlines.values()) {
            String target = wsuser.getUid();
            if (!target.equals(uid) && exclude.stream().noneMatch(target::equals)) {
                wsmsg.send(wsuser, Remote.ASYNC);
            }
        }
    }

    /**
     * 向所有用户发送系统消息（排除自己）
     *
     * @param tips   消息内容
     * @param type   消息类型
     * @param sender 发起者信息
     */
    public void sendAll(Callback tips, MessageType type, SysUser sender) {
        WsMsg sysmsg = WsMsg.build(tips, type, sender);
        for (WsUser wsuser : onlines.values()) {
            if (!wsuser.getUid().equals(sender.getUid())) {
                sysmsg.send(wsuser, Remote.ASYNC);
            }
        }
    }

    /**
     * 通过uid获取在线用户
     *
     * @param uid 用户uid
     * @return {@link WsUser}
     */
    public WsUser getOnline(String uid) {
        return onlines.get(uid);
    }

    /**
     * 通过uid获取用户信息
     *
     * @param uid 用户uid
     * @return {@link WsUser}
     */
    public WsUser getUser(String uid) {
        WsUser onlineUser = getOnline(uid);
        if (onlineUser != null) {
            return onlineUser;
        }
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUid, uid);
        wrapper.eq(SysUser::isDeleted, 0);
        SysUser sysUser = sysUserMapper.selectOne(wrapper);
        return Opt.ofNullable(sysUser).map(WsUser::new).get();
    }

    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public Collection<UserPreview> getUserList(WsUser self) {
        // 用户列表
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
        wrapper.eq(SysUser::isDeleted, 0);
        List<SysUser> userList = sysUserMapper.selectList(wrapper);
        // 消息发起者
        String suid = self.getUid();
        // 与此用户关联的所有未读消息
        Map<String, SortedSet<ChatRecord>> messages = recordService.getUnreadMessages(suid);
        // 登录记录
        Map<String, Date> logs = this.getUserLoginLogs();
        // 链接数据
        List<UserPreview> collect = userList.stream()
                .map(UserPreview::new)
                // 补全状态
                .peek(user -> user.fill(logs, onlines))
                // 同步未读消息
                .peek(user -> this.syncUnreadMessage(user, messages, suid))
                // 转为List
                .collect(Collectors.toList());
        // 添加游客到列表（数据库不包含游客信息）
        onlines.values().stream()
                .filter(WsUser::isGuest)
                .map(UserPreview::new)
                .forEach(collect::add);
        // 查找自己并设置屏蔽列表
        collect.stream()
                .filter(user -> user.getUid().equals(suid))
                .findFirst()
                .ifPresent(user -> user.setShields(redisManager.getShield(user.getUid())));
        return collect;
    }

    /**
     * 获取所有用户登录最新时间
     */
    private Map<String, Date> getUserLoginLogs() {
        QueryWrapper<SysUserLog> w2 = Wrappers.query();
        w2.select(Wss.columnToString(SysUserLog::getUid), Wss.selecterMax(BaseModel::getCreateTime));
        w2.lambda().groupBy(SysUserLog::getUid);
        List<SysUserLog> userLogs = sysUserLogMapper.selectList(w2);
        return userLogs.stream().collect(Collectors.toMap(SysUserLog::getUid, BaseModel::getCreateTime));
    }

    /**
     * 同步未读消息
     */
    private void syncUnreadMessage(UserPreview preview, Map<String, SortedSet<ChatRecord>> message, String sender) {
        String target = preview.getUid();
        int count = redisManager.getUnreadCount(sender, target);
        if (count > 0) {
            SortedSet<ChatRecord> records = message.get(target);
            ChatRecord first;
            if (records != null && (first = records.first()) != null) {
                MessageType type = first.getType();
                preview.setPreview(type == MessageType.TEXT ? first.getContent() : '[' + type.getPreview() + ']');
                preview.setLastTime(first.getCreateTime().getTime());
                preview.setUnreads(Math.min(count, 99));
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
     * 连续发言标记 <br>
     * 10秒内超过一定次数会被禁止一段时间发言（忽略身份）
     */
    public void operateMark(WsUser user) {
        int time = Constants.FREQUENT_SPEECHES_MUTE_TIME;
        if (redisManager.incrSpeak(user.getUid()) > Constants.FREQUENT_SPEECH_THRESHOLD) {
            redisManager.setMute(user.getUid(), time);
            WsMsg.build(Callback.BRUSH_SCREEN.format(time), MessageType.MUTE, time).send(user, Remote.ASYNC);
        }
    }


    /**
     * 保存聊天记录
     *
     * @param wsmsg  聊天消息
     * @param isread 已读标记
     */
    public void cacheRecord(WsMsg wsmsg, boolean isread) {
        ChatRecord record = BeanUtil.copyProperties(wsmsg, ChatRecord.class);
        // 群组以外的语音消息始终未读
        boolean audio = !wsmsg.isGroup() && wsmsg.getType() == MessageType.AUDIO;
        record.setUnread(audio || !isread);
        kafkaTemplate.send(Constants.KAFKA_RECORD, JSONUtil.toJsonStr(record));
        // 目标列表添加发起者uid
        if (!isread) {
            redisManager.setUnreadCount(wsmsg.getTarget(), wsmsg.getUid(), 1);
        }
    }

    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(WsUser secure, WsUser target) {
        return redisManager.getShield(secure.getUid()).contains(target.getUid());
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
        List<String> shields = redisManager.getShield(uid);
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
     * 更新发起人的消息为已读
     *
     * @param sender 发起人
     * @param target 目标
     * @param audio  是否包括语音消息
     */
    public void readAllMessage(WsUser sender, WsUser target, boolean audio) {
        String suid = sender.getUid(), tuid = target.getUid();
        recordService.readAllMessage(suid, tuid, audio);
        redisManager.setUnreadCount(suid, tuid, 0);
    }

    /**
     * 撤回消息
     *
     * @param wsmsg 消息
     * @return 撤回的消息
     */
    public ChatRecord removeMessage(WsMsg wsmsg) {
        ChatRecord record = recordService.withdrawMessage(wsmsg.getUid(), wsmsg.getContent());
        // 未找到相关消息
        if (record == null) {
            return null;
        }
        // 未读消息计数器-1
        if (record.isUnread()) {
            redisManager.setUnreadCount(wsmsg.getTarget(), wsmsg.getUid(), -1);
        }
        return record;
    }

    /**
     * 获取指定用户的未读消息数量
     *
     * @param sender 自己
     * @param target 目标
     * @return 未读消息数
     */
    public int getUnreadCount(WsUser sender, String target) {
        return redisManager.getUnreadCount(sender.getUid(), target);
    }

    /**
     * 检查指定用户禁言情况，若用户被禁言将发送一条系统通知
     */
    public void checkMute(WsUser user) {
        long muteTime = redisManager.getMuteTime(user.getUid());
        if (muteTime > 0) {
            Callback tips = Callback.MUTE_LIMIT.format(muteTime);
            WsMsg.build(tips, MessageType.MUTE, muteTime).send(user, Remote.ASYNC);
        }
    }

    /**
     * 移除在线用户
     *
     * @param user 用户信息
     */
    public void remove(WsUser user) {
        user.logout(null);
        onlines.remove(user.getUid());
    }

    /**
     * 推送公告
     *
     * @param wsMsg 系统消息
     */
    public void pushNotice(WsMsg wsMsg, SysUser sender) {
        String content = wsMsg.getContent();
        redisManager.pushNotice(content);
        if (StrUtil.isNotEmpty(content)) {
            this.sendAll(Callback.MANUAL.format(content), MessageType.ANNOUNCE, sender);
        }
    }
}
