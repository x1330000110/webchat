package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.custom.XiaoBingRequest;
import com.socket.client.feign.ChatRecordApi;
import com.socket.client.feign.SysUserLogApi;
import com.socket.client.model.SocketMessage;
import com.socket.client.model.SocketUser;
import com.socket.core.constant.ChatConstants;
import com.socket.core.constant.Topics;
import com.socket.core.custom.SocketRedisManager;
import com.socket.core.custom.TokenUserManager;
import com.socket.core.mapper.SysUserMapper;
import com.socket.core.model.AuthUser;
import com.socket.core.model.command.Command;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.condition.MessageCondition;
import com.socket.core.model.enums.LogType;
import com.socket.core.model.po.ChatRecord;
import com.socket.core.model.po.SysUser;
import com.socket.core.model.po.SysUserLog;
import com.socket.core.util.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.yeauty.pojo.Session;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ws用户管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserManager extends ConcurrentHashMap<String, SocketUser> {
    private final KafkaTemplate<String, String> messageQueue;
    private final XiaoBingRequest xiaoBingRequest;
    private final TokenUserManager tokenUserManager;
    private final SocketRedisManager redisManager;
    private final SysUserLogApi sysUserLogApi;
    private final ChatRecordApi chatRecordApi;
    private final SysUserMapper userMapper;
    private final ChatConstants constants;

    /**
     * 加入用户
     *
     * @param session ws session
     * @param token   令牌
     * @return 已加入的用户对象
     */
    public SocketUser join(Session session, String token) {
        // 通过令牌查找用户
        AuthUser auth = tokenUserManager.getTokenUser(token);
        if (auth == null) {
            return null;
        }
        // 查找用户
        SocketUser user = get(auth.getUid());
        // 检查重复登录
        if (user.isOnline()) {
            user.logout("您的账号已在别处登录");
        }
        // 写入聊天室
        user.login(tokenUserManager, session, token);
        // 检查登录限制（会话缓存检查）
        long time = redisManager.getLockTime(user.getGuid());
        if (time > 0) {
            this.exit(user, StrUtil.format("您已被管理员限制登陆{}", time));
            return null;
        }
        // 记录登录信息
        SysUserLog userLog = BeanUtil.copyProperties(user, SysUserLog.class);
        userLog.setIp(user.getIp());
        userLog.setType(Enums.key(LogType.LOGIN));
        sysUserLogApi.saveLog(userLog);
        return user;
    }

    /**
     * 通过uid获取用户（不存在时通过数据库获取）
     *
     * @param uid 用户uid
     * @return {@link SocketUser}
     */
    public SocketUser get(String uid) {
        return Optional.ofNullable(this.get((Object) uid)).orElseGet(() -> {
            // 从数据库查询
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getGuid, uid);
            SysUser find = userMapper.selectOne(wrapper);
            SocketUser user = Optional.ofNullable(find).map(SocketUser::new).orElse(null);
            // 写入缓存
            Optional.ofNullable(user).ifPresent(e -> this.put(uid, e));
            return user;
        });
    }

    /**
     * 用户退出
     *
     * @param user   用户
     * @param reason 原因
     */
    public void exit(SocketUser user, String reason) {
        // 保存日志
        SysUserLog log = BeanUtil.copyProperties(user, SysUserLog.class);
        log.setIp(user.getIp());
        log.setType(Enums.key(LogType.LOGOUT));
        sysUserLogApi.saveLog(log);
        // 注销目标会话
        user.logout(reason);
    }

    /**
     * @see #sendAll(String, Command, Object)
     */
    public void sendAll(String content, Command<?> command) {
        this.sendAll(content, command, null);
    }

    /**
     * 向所有用户发送系统消息
     *
     * @param content 消息内容
     * @param command 消息类型
     * @param data    附加用户信息
     */
    public void sendAll(String content, Command<?> command, Object data) {
        this.values().forEach(user -> user.send(content, command, data));
    }

    /**
     * @see #sendAll(String, Command, Object)
     */
    public void sendAll(Command<?> command, Object data) {
        this.sendAll(null, command, data);
    }

    /**
     * 更新发起人的消息为已读
     *
     * @param self   发起人
     * @param target 目标
     * @param audio  是否包括语音消息
     */
    public void readAllMessage(String self, String target, boolean audio) {
        MessageCondition condition = new MessageCondition();
        condition.setGuid(self);
        condition.setTarget(target);
        condition.setAudio(audio);
        chatRecordApi.readAllMessage(condition);
        redisManager.setUnreadCount(self, target, 0);
    }

    /**
     * 获取指定用户的未读消息数量
     *
     * @param self   自己
     * @param target 目标
     * @return 未读消息数
     */
    public int getUnreadCount(String self, String target) {
        return redisManager.getUnreadCount(self, target);
    }

    /**
     * 发送AI消息
     *
     * @param target  发送目标
     * @param message 消息
     */
    public void sendAIMessage(SocketUser target, SocketMessage message) {
        boolean sysuid = constants.getSystemUid().equals(message.getTarget());
        boolean text = CommandEnum.TEXT == message.getType();
        // 判断AI消息
        if (sysuid && text && !get(constants.getSystemUid()).isOnline()) {
            xiaoBingRequest.dialogue(message.getContent()).addCallback(result -> {
                if (result != null) {
                    // AI消息
                    SocketMessage aimsg = new SocketMessage(constants.getSystemUid(), message.getGuid(), result, CommandEnum.TEXT);
                    target.send(aimsg);
                    cacheRecord(aimsg, true);
                }
            }, exception -> log.warn(exception.getMessage()));
        }
    }

    /**
     * 保存聊天记录
     *
     * @param message 聊天消息
     * @param isread  已读标记
     */
    public void cacheRecord(SocketMessage message, boolean isread) {
        ChatRecord record = BeanUtil.copyProperties(message, ChatRecord.class);
        // 群组以外的语音消息始终未读
        boolean audio = !message.isGroup() && message.getType() == CommandEnum.AUDIO;
        record.setUnread(audio || !isread);
        messageQueue.send(Topics.MESSAGE, JSONUtil.toJsonStr(record));
        // 目标列表添加发起者uid
        if (!isread) {
            redisManager.setUnreadCount(message.getTarget(), message.getGuid(), 1);
        }
    }
}
