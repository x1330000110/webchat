package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.exception.SocketException;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.support.keyword.SensitiveKeywordShieldSupport;
import com.socket.client.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.listener.UserChangeEvent;
import com.socket.webchat.custom.listener.UserChangeListener;
import com.socket.webchat.mapper.*;
import com.socket.webchat.model.*;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.request.XiaoBingAPIRequest;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.InitializingBean;
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
public class SocketManager implements InitializingBean, UserChangeListener {
    private final ConcurrentHashMap<SysGroup, List<String>> groups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WsUser> users = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysUserLogMapper sysUserLogMapper;
    private final ShieldUserMapper shieldUserMapper;
    private final SysGroupMapper sysGroupMapper;
    private final RecordService recordService;
    private final SysUserMapper sysUserMapper;
    private final RedisManager redisManager;

    private final SensitiveKeywordShieldSupport keywordSupport;
    private final XiaoBingAPIRequest request;

    /**
     * 加入用户
     *
     * @param session    ws session
     * @param properties 配置信息
     * @return 已加入的用户对象
     */
    public WsUser join(Session session, Map<String, Object> properties) {
        // 查找用户
        Subject subject = (Subject) properties.get(Constants.SUBJECT);
        SysUser principal = (SysUser) subject.getPrincipal();
        WsUser user = getUser(principal.getUid());
        // 检查登录限制（会话缓存检查）
        long time = redisManager.getLockTime(user.getUid());
        if (time > 0) {
            user.logout(Callback.LOGIN_LIMIT.format(time));
            return null;
        }
        // 检查重复登录
        if (user.isOnline()) {
            user.logout(Callback.REPEAT_LOGIN.get());
        }
        // 登录到聊天室
        HttpSession hs = (HttpSession) properties.get(Constants.HTTP_SESSION);
        String platform = (String) properties.get(Constants.PLATFORM);
        user.login(session, hs, platform);
        return user;
    }

    /**
     * 向群组发送消息（不包括发起者和屏蔽的用户）<br>
     *
     * @param wsmsg  消息
     * @param sender 发起者
     */
    public void sendGroup(WsMsg wsmsg, WsUser sender) {
        List<String> exclude = redisManager.getShield(sender.getUid());
        String uid = sender.getUid();
        // 向群内所有人发送消息
        for (String tuid : groups.get(getSysGroup(wsmsg.getTarget()))) {
            // 过滤自己 || 已屏蔽
            if (uid.equals(tuid) || exclude.contains(tuid)) {
                continue;
            }
            // 发送
            getUser(tuid).send(wsmsg);
        }
    }

    /**
     * 向所有用户发送系统消息（排除自己）
     *
     * @param content 消息内容
     * @param type    消息类型
     * @param sender  发起者信息
     */
    public void sendAll(String content, MessageType type, SysUser sender) {
        WsMsg sysmsg = new WsMsg(content, type, sender);
        for (WsUser wsuser : users.values()) {
            if (!wsuser.getUid().equals(sender.getUid())) {
                wsuser.send(sysmsg);
            }
        }
    }

    /**
     * 通过uid获取在线用户
     *
     * @param uid 用户uid
     * @return {@link WsUser}
     */
    public WsUser getUser(String uid) {
        WsUser user = users.get(uid);
        Assert.notNull(user, Callback.USER_NOT_FOUND);
        return user;
    }

    /**
     * 通过消息获取目标身份
     *
     * @param wsmsg 消息
     * @return {@link WsUser}
     * @throws SocketException 找不到用户
     */
    public WsUser getTarget(WsMsg wsmsg) {
        String target = wsmsg.getTarget();
        // 群组
        if (wsmsg.isGroup()) {
            return new WsUser(getSysGroup(target).toSysUser());
        }
        // 查找用户
        return getUser(target);
    }

    /**
     * 获取群组对象
     */
    private SysGroup getSysGroup(String groupId) {
        return groups.keySet().stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new SocketException(Callback.USER_NOT_FOUND.get(), MessageType.DANGER));
    }

    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public Collection<UserPreview> getPreviews(WsUser self) {
        // 消息发起者
        String suid = self.getUid();
        // 与此用户关联的所有未读消息
        Map<String, SortedSet<ChatRecord>> messages = recordService.getUnreadMessages(suid);
        // 登录记录
        Map<String, Date> logs = this.getUserLoginLogs();
        // 链接数据
        List<UserPreview> collect = users.values().stream()
                .map(UserPreview::new)
                .peek(user -> user.setLastTime(logs))
                .peek(user -> this.syncUnreadMessage(user, messages, suid))
                .collect(Collectors.toList());
        // 添加群组到列表
        for (Map.Entry<SysGroup, List<String>> entry : groups.entrySet()) {
            SysGroup group = entry.getKey();
            List<String> uids = entry.getValue();
            // 需要在群里
            if (uids.contains(suid)) {
                UserPreview preview = new UserPreview();
                preview.setGroup(true);
                preview.setMembers(uids);
                preview.setUid(group.getGroupId());
                preview.setName(group.getName());
                preview.setOnline(true);
                collect.add(preview);
            }
        }
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
        QueryWrapper<SysUserLog> wrapper = Wrappers.query();
        wrapper.select(Wss.columnToString(SysUserLog::getUid), Wss.selecterMax(BaseModel::getCreateTime));
        wrapper.lambda().groupBy(SysUserLog::getUid);
        List<SysUserLog> userLogs = sysUserLogMapper.selectList(wrapper);
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
                preview.setLastTime(first.getCreateTime());
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
            user.send(Callback.BRUSH_SCREEN.format(time), MessageType.MUTE, time);
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
    public ChatRecord withdrawMessage(WsMsg wsmsg) {
        ChatRecord record = recordService.removeMessage(wsmsg.getUid(), wsmsg.getContent());
        // 未读计数器-1
        Optional.ofNullable(record).filter(ChatRecord::isUnread).ifPresent(msg -> {
            redisManager.setUnreadCount(msg.getTarget(), msg.getUid(), -1);
        });
        return record;
    }

    /**
     * 获取指定用户的未读消息数量
     *
     * @param sender 自己
     * @param target 目标
     * @return 未读消息数
     */
    public int getUnreadCount(WsUser sender, WsUser target) {
        return redisManager.getUnreadCount(sender.getUid(), target.getUid());
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
     * 移除在线用户
     *
     * @param user        用户信息
     * @param deleteCache 删除缓存
     */
    public void remove(WsUser user, boolean deleteCache) {
        user.logout(null);
        if (deleteCache) {
            users.remove(user.getUid());
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
            this.sendAll(content, MessageType.ANNOUNCE, sender);
        }
    }

    /**
     * 初始化数据
     */
    @Override
    public void afterPropertiesSet() {
        // 缓存群组
        List<SysGroup> groups = sysGroupMapper.selectList(Wrappers.emptyWrapper());
        List<SysGroupUser> groupUsers = sysGroupUserMapper.selectList(Wrappers.emptyWrapper());
        for (SysGroup group : groups) {
            List<String> collect = groupUsers.stream()
                    .filter(e -> e.getGroupId().equals(group.getGroupId()))
                    .map(SysGroupUser::getUid)
                    .collect(Collectors.toList());
            this.groups.put(group, collect);
        }
        // 缓存用户
        List<SysUser> userList = sysUserMapper.selectList(Wrappers.emptyWrapper());
        userList.stream().map(WsUser::new).forEach(e -> users.put(e.getUid(), e));
    }

    @Override
    public void onUserChange(UserChangeEvent event) {
        SysUser user = event.getUser();
        String uid = user.getUid();
        SysUser ws = users.get(uid);
        // 用户存在则更新资料
        if (ws != null) {
            Optional.ofNullable(user.getName()).ifPresent(ws::setName);
            Optional.ofNullable(user.getHeadimgurl()).ifPresent(ws::setHeadimgurl);
            return;
        }
        // 添加到缓存
        WsUser wsuser = new WsUser(user);
        users.put(uid, wsuser);
        // 检查默认群组是否存在新用户（不存在添加）
        List<String> groupUsers = groups.get(getSysGroup(Constants.GROUP));
        if (!groupUsers.contains(uid)) {
            groupUsers.add(uid);
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
        content = content.replaceAll("</?\\w+(\\s.+?)?>", "");
        content = StrUtil.sub(content, 0, Constants.MAX_MESSAGE_LENGTH);
        if (sensitive && keywordSupport.containsSensitive(content)) {
            wsuser.reject(Callback.SENSITIVE_KEYWORDS, wsmsg);
            return false;
        }
        wsmsg.setContent(content);
        return true;
    }

    /**
     * 发送AI消息
     *
     * @param target 发送目标
     * @param wsmsg  消息
     */
    public void sendAIMessage(WsUser target, WsMsg wsmsg) {
        request.dialogue(wsmsg.getContent()).addCallback(result -> {
            if (result != null) {
                // AI消息
                WsMsg aimsg = new WsMsg(Constants.SYSTEM_UID, wsmsg.getUid(), result, MessageType.TEXT);
                target.send(aimsg);
                cacheRecord(aimsg, true);
            }
        }, exception -> log.warn(exception.getMessage()));
    }
}
