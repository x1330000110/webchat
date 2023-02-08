package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.CommandEnum;
import com.socket.webchat.model.enums.LogType;
import com.socket.webchat.request.XiaoBingAPIRequest;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.service.SysUserLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ws用户管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocketUserMap extends ConcurrentHashMap<String, WsUser> {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final XiaoBingAPIRequest xiaoBingAPIRequest;
    private final SysUserLogService logService;
    private final RecordService recordService;
    private final SysUserMapper userMapper;
    private final RedisManager redisManager;

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
        WsUser user = get(principal.getGuid());
        HttpSession hs = (HttpSession) properties.get(Constants.HTTP_SESSION);
        // 检查重复登录
        if (user.isOnline() && user.differentSession(hs)) {
            user.logout("您的账号已在别处登录");
        }
        // 写入聊天室
        user.login(session, hs, subject);
        // 检查登录限制（会话缓存检查）
        long time = redisManager.getLockTime(user.getGuid());
        if (time > 0) {
            this.exit(user, StrUtil.format("您已被管理员限制登陆{}", time));
            return null;
        }
        // 记录登录信息
        SysUserLog userLog = BeanUtil.copyProperties(user, SysUserLog.class);
        userLog.setIp(user.getIp());
        logService.saveLog(userLog, LogType.LOGIN);
        return user;
    }

    /**
     * 通过uid获取用户（不存在时通过数据库获取）
     *
     * @param uid 用户uid
     * @return {@link WsUser}
     */
    public WsUser get(String uid) {
        return Optional.ofNullable(this.get((Object) uid)).orElseGet(() -> {
            // 从数据库查询
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysUser::getGuid, uid);
            SysUser find = userMapper.selectOne(wrapper);
            WsUser wsuser = Optional.ofNullable(find).map(WsUser::new).orElse(null);
            // 写入缓存
            Optional.ofNullable(wsuser).ifPresent(e -> this.put(uid, e));
            return wsuser;
        });
    }

    /**
     * 用户退出
     *
     * @param user   用户
     * @param reason 原因
     */
    public void exit(WsUser user, String reason) {
        SysUserLog log = BeanUtil.copyProperties(user, SysUserLog.class);
        log.setIp(user.getIp());
        logService.saveLog(log, LogType.LOGOUT);
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
        this.values().forEach(wsuser -> wsuser.send(content, command, data));
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
        recordService.readAllMessage(self, target, audio);
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
     * @param target 发送目标
     * @param wsmsg  消息
     */
    public void sendAIMessage(WsUser target, WsMsg wsmsg) {
        xiaoBingAPIRequest.dialogue(wsmsg.getContent()).addCallback(result -> {
            if (result != null) {
                // AI消息
                WsMsg aimsg = new WsMsg(Constants.SYSTEM_UID, wsmsg.getGuid(), result, CommandEnum.TEXT);
                target.send(aimsg);
                cacheRecord(aimsg, true);
            }
        }, exception -> log.warn(exception.getMessage()));
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
        boolean audio = !wsmsg.isGroup() && Objects.equals(wsmsg.getType(), CommandEnum.AUDIO.getCommand());
        record.setUnread(audio || !isread);
        kafkaTemplate.send(Constants.KAFKA_RECORD, JSONUtil.toJsonStr(record));
        // 目标列表添加发起者uid
        if (!isread) {
            redisManager.setUnreadCount(wsmsg.getTarget(), wsmsg.getGuid(), 1);
        }
    }
}
