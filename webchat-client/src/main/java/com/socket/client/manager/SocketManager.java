package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.model.enums.CallbackTips;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.mapper.ShieldUserMapper;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ShieldUser;
import com.socket.webchat.model.SysUser;
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
    private final ConcurrentHashMap<String, WsUser> onlineUsers = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ShieldUserMapper shieldUserMapper;
    private final RecordService recordService;
    private final SysUserMapper sysUserMapper;
    private final RedisManager redisManager;

    /**
     * 加入用户
     *
     * @return 已加入的用户对象
     */
    public WsUser join(Subject subject, Session socketSession, HttpSession httpSession) {
        // 构建聊天室用户
        WsUser user = new WsUser(subject, socketSession, httpSession);
        // 检查登录限制（会话缓存检查）
        long time = redisManager.getLockTime(user.getUid());
        if (time > 0) {
            user.logout(CallbackTips.LOGIN_LIMIT.of(time));
            return null;
        }
        // 检查重复登录
        WsUser varuser = onlineUsers.get(user.getUid());
        if (varuser != null) {
            varuser.logout(CallbackTips.REPEAT_LOGIN.of());
        }
        // 登录到聊天室
        onlineUsers.put(user.getUid(), user);
        return user;
    }

    /**
     * 向所有用户发送消息（不包括发起者和屏蔽的用户）<br>
     *
     * @param wsmsg  消息
     * @param sender 发起者
     */
    public void sendAll(WsMsg wsmsg, WsUser sender) {
        List<Object> exclude = Arrays.asList(getShield(sender).toArray(), wsmsg.getUid());
        for (WsUser wsuser : onlineUsers.values()) {
            String target = wsuser.getUid();
            if (exclude.stream().noneMatch(target::equals)) {
                wsmsg.sendTo(wsuser);
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
    public void sendAll(CallbackTips tips, MessageType type, SysUser sender) {
        WsMsg sysmsg = WsMsg.buildsys(tips, type, sender);
        for (WsUser wsuser : onlineUsers.values()) {
            if (!wsuser.getUid().equals(sender.getUid())) {
                sysmsg.sendTo(wsuser);
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
        return onlineUsers.get(uid);
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
     * @param sender 当前登录的用户
     */
    public Collection<UserPreview> getUserList(WsUser sender) {
        // 用户列表
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
        wrapper.eq(SysUser::isDeleted, 0);
        List<SysUser> userList = sysUserMapper.selectList(wrapper);
        // 消息发起者
        String senderUid = sender.getUid();
        // 与此用户关联的所有未读消息
        Map<String, SortedSet<ChatRecord>> messagesMap = recordService.getUnreadMessages(senderUid);
        // 链接数据
        List<UserPreview> collect = new ArrayList<>();
        for (SysUser sysUser : userList) {
            UserPreview preview = new UserPreview(sysUser);
            preview.setOnline(onlineUsers.get(preview.getUid()) != null);
            String uid = preview.getUid();
            // 用户为自己 添加屏蔽列表
            if (uid.equals(senderUid)) {
                preview.setShields(this.getShield(sender));
                collect.add(preview);
                continue;
            }
            // 从Redis获取未读消息（Redis统计里忽略了语音消息）
            int count = redisManager.getUnreadCount(senderUid, uid);
            if (count > 0) {
                SortedSet<ChatRecord> records = messagesMap.get(uid);
                ChatRecord first;
                if (records != null && (first = records.first()) != null) {
                    preview.setPreview(this.parseMessage(first));
                    preview.setLastTime(first.getCreateTime().getTime());
                    preview.setUnreadCount(Math.min(records.size(), 99));
                }
            }
            // 清除敏感信息
            preview.setIp(null).setHash(null).setPlatform(preview.isOnline() ? preview.getPlatform() : null);
            collect.add(preview);
        }
        return collect;
    }

    /**
     * parse preview message
     */
    private String parseMessage(ChatRecord record) {
        MessageType type = record.getType();
        return type == MessageType.TEXT ? record.getContent() : '[' + type.getPreview() + ']';
    }

    /**
     * 检查指定用户是否被禁言
     *
     * @return true被禁言
     */
    public boolean isMute(String uid) {
        return redisManager.getMuteTime(uid) > 0;
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
            WsMsg.buildsys(CallbackTips.MALICIOUS_SPEAK.of(time), MessageType.MUTE, time).sendTo(user);
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
        // 未读消息计数器
        if (!isread) {
            // 目标列表添加发起者uid
            redisManager.setUnreadCount(wsmsg.getTarget(), wsmsg.getUid(), 1);
        }
    }

    /**
     * 获取指定用户的屏蔽列表（优先从缓存加载）<br>
     *
     * @param user 用户信息
     */
    private List<String> getShield(WsUser user) {
        // 初始化屏蔽列表
        if (user.getShields() == null) {
            LambdaQueryWrapper<ShieldUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(ShieldUser::getUid, user.getUid());
            wrapper.eq(ShieldUser::isDeleted, 0);
            List<ShieldUser> users = shieldUserMapper.selectList(wrapper);
            List<String> collect = users.stream().map(ShieldUser::getTarget).collect(Collectors.toList());
            user.setShields(collect);
        }
        return user.getShields();
    }

    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(WsUser secure, WsUser target) {
        return getShield(secure).contains(target.getUid());
    }

    /**
     * 屏蔽/取消屏蔽 指定用户
     *
     * @param user   用户信息
     * @param target 目标用户
     * @return 若成功屏蔽返回true, 取消屏蔽返回false
     */
    public boolean shieldTarget(WsUser user, WsUser target) {
        List<String> shields = getShield(user);
        String tuid = target.getUid();
        LambdaUpdateWrapper<ShieldUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ShieldUser::getUid, user.getUid());
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
            suser.setUid(user.getUid());
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
            CallbackTips tips = CallbackTips.MUTE_LIMIT.of(muteTime);
            WsMsg.buildsys(tips, MessageType.MUTE, muteTime).sendTo(user);
        }
    }

    /**
     * 移除在线用户
     *
     * @param user 用户信息
     */
    public void remove(WsUser user) {
        user.logout(null);
        onlineUsers.remove(user.getUid());
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
            this.sendAll(CallbackTips.MANUAL.of(content), MessageType.ANNOUNCE, sender);
        }
    }
}
