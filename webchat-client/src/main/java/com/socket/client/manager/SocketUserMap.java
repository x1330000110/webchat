package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.MessageEnum;
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
            this.exit(user, Callback.LOGIN_LIMIT.format(time));
            return null;
        }
        // 记录登录信息
        logService.saveLog(BeanUtil.copyProperties(user, SysUserLog.class), LogType.LOGIN);
        return user;
    }

    /**
     * 向所有用户发送系统消息
     *
     * @param content 消息内容
     * @param type    消息类型
     * @param data    附加用户信息
     */
    public void sendAll(Object content, Command<?> type, Object data) {
        String constr = content == null ? null : content.toString();
        this.values().forEach(wsuser -> wsuser.send(constr, type, data));
    }

    /**
     * @see #sendAll(Object, Command, Object)
     */
    public void sendAll(Object content, Command<?> type) {
        this.sendAll(content, type, null);
    }

    /**
     * @see #sendAll(Object, Command, Object)
     */
    public void sendAll(Command<?> type, Object data) {
        this.sendAll(null, type, data);
    }

    /**
     * 通过uid获取用户（不存在时通过缓存获取）
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
     * 保存聊天记录
     *
     * @param wsmsg  聊天消息
     * @param isread 已读标记
     */
    public void cacheRecord(WsMsg wsmsg, boolean isread) {
        ChatRecord record = BeanUtil.copyProperties(wsmsg, ChatRecord.class);
        // 群组以外的语音消息始终未读
        boolean audio = !wsmsg.isGroup() && Objects.equals(wsmsg.getType(), MessageEnum.AUDIO.getName());
        record.setUnread(audio || !isread);
        kafkaTemplate.send(Constants.KAFKA_RECORD, JSONUtil.toJsonStr(record));
        // 目标列表添加发起者uid
        if (!isread) {
            redisManager.setUnreadCount(wsmsg.getTarget(), wsmsg.getGuid(), 1);
        }
    }


    /**
     * 更新发起人的消息为已读
     *
     * @param self   发起人
     * @param target 目标
     * @param audio  是否包括语音消息
     */
    public void readAllMessage(WsUser self, WsUser target, boolean audio) {
        String suid = self.getGuid();
        String tuid = target.getGuid();
        recordService.readAllMessage(suid, tuid, audio);
        redisManager.setUnreadCount(suid, tuid, 0);
    }


    /**
     * 获取指定用户的未读消息数量
     *
     * @param self   自己
     * @param target 目标
     * @return 未读消息数
     */
    public int getUnreadCount(WsUser self, WsUser target) {
        return redisManager.getUnreadCount(self.getGuid(), target.getGuid());
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
                WsMsg aimsg = new WsMsg(Constants.SYSTEM_UID, wsmsg.getGuid(), result, MessageEnum.TEXT);
                target.send(aimsg);
                cacheRecord(aimsg, true);
            }
        }, exception -> log.warn(exception.getMessage()));
    }

    /**
     * 用户退出
     *
     * @param user   用户
     * @param reason 原因
     */
    public void exit(WsUser user, String reason) {
        logService.saveLog(BeanUtil.copyProperties(user, SysUserLog.class), LogType.LOGOUT);
        user.logout(reason);
    }
}
