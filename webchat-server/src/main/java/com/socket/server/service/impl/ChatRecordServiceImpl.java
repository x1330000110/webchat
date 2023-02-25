package com.socket.server.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.core.constant.Constants;
import com.socket.core.constant.Topics;
import com.socket.core.custom.RedisManager;
import com.socket.core.mapper.ChatRecordDeletedMapper;
import com.socket.core.mapper.ChatRecordMapper;
import com.socket.core.mapper.ChatRecordOffsetMapper;
import com.socket.core.model.base.BaseModel;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.po.ChatRecord;
import com.socket.core.model.po.ChatRecordDeleted;
import com.socket.core.model.po.ChatRecordOffset;
import com.socket.core.util.DbUtil;
import com.socket.core.util.ShiroUser;
import com.socket.core.util.Wss;
import com.socket.secure.util.Assert;
import com.socket.server.custom.publisher.CommandPublisher;
import com.socket.server.service.ChatRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements ChatRecordService {
    private final ChatRecordDeletedMapper deletedMapper;
    private final ChatRecordOffsetMapper offsetMapper;
    private final CommandPublisher publisher;
    private final RedisManager redisManager;

    @KafkaListener(topics = Topics.MESSAGE, groupId = "MESSAGE")
    public void saveRecord(ConsumerRecord<String, String> data) {
        ChatRecord record = JSONUtil.toBean(data.value(), ChatRecord.class);
        super.save(record);
    }

    @Override
    public List<ChatRecord> getRecords(String mid, String target) {
        String userId = ShiroUser.getUserId();
        LambdaQueryWrapper<ChatRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecord::isSysmsg, 0);

        // 排除已删除的消息id
        LambdaQueryWrapper<ChatRecordDeleted> exclude = Wrappers.lambdaQuery();

        // 若查询群组，获取所有目标为群组的消息
        if (Wss.isGroup(target)) {
            wrapper.eq(ChatRecord::isReject, false);
            wrapper.eq(ChatRecord::getTarget, target);
            exclude.eq(ChatRecordDeleted::getTarget, target);
        } else {
            // 反之仅获取相互发送的消息
            wrapper.and(ew -> ew.eq(ChatRecord::isReject, false).or().eq(ChatRecord::getGuid, userId));
            wrapper.and(ew -> ew
                    .eq(ChatRecord::getGuid, userId)
                    .eq(ChatRecord::getTarget, target)
                    .or()
                    .eq(ChatRecord::getGuid, target)
                    .eq(ChatRecord::getTarget, userId));
            exclude.and(ew -> ew
                    .eq(ChatRecordDeleted::getGuid, userId)
                    .eq(ChatRecordDeleted::getTarget, target)
                    .or()
                    .eq(ChatRecordDeleted::getGuid, target)
                    .eq(ChatRecordDeleted::getTarget, userId));
        }

        // 限制起始边界id
        LambdaQueryWrapper<ChatRecordOffset> start = Wrappers.lambdaQuery();
        start.eq(ChatRecordOffset::getGuid, userId);
        start.eq(ChatRecordOffset::getTarget, target);
        ChatRecordOffset limit = offsetMapper.selectOne(start);
        if (limit != null) {
            wrapper.gt(BaseModel::getId, limit.getOffset());
            exclude.ge(ChatRecordDeleted::getRecordId, limit.getOffset());
        }

        // 限制结束边界id
        if (mid != null) {
            // 通过mid查询id
            LambdaQueryWrapper<ChatRecord> end = Wrappers.lambdaQuery();
            end.eq(ChatRecord::getMid, mid);
            ChatRecord offset = getFirst(end);
            Assert.notNull(offset, "无效的MID", IllegalStateException::new);
            wrapper.lt(BaseModel::getId, offset.getId());
            exclude.le(ChatRecordDeleted::getRecordId, offset.getId());
        }

        // 将单独删除的消息转为集合
        List<Long> deleted = deletedMapper.selectList(exclude)
                .stream()
                .map(ChatRecordDeleted::getRecordId)
                .collect(Collectors.toList());
        wrapper.notIn(!deleted.isEmpty(), ChatRecord::getId, deleted);

        // 限制结果
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        wrapper.last(StrUtil.format("LIMIT {}", Constants.SYNC_RECORDS_NUMS));
        return list(wrapper);
    }

    @Override
    public boolean withdrawMessage(String mid) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::getGuid, ShiroUser.getUserId());
        wrapper.eq(ChatRecord::getMid, mid);
        ChatRecord record = getFirst(wrapper);
        Assert.notNull(record, "服务器消息同步中，请稍后再试", IllegalStateException::new);
        // 消息未送达或未超过规定撤回时间
        long second = DateUtil.between(record.getCreateTime(), new Date(), DateUnit.SECOND);
        if (record.isReject() || second <= Constants.WITHDRAW_TIME) {
            wrapper.set(BaseModel::isDeleted, 1);
            update(wrapper);
            // 若消息未读 计数器-1
            if (record.isUnread()) {
                redisManager.setUnreadCount(record.getTarget(), record.getGuid(), -1);
            }
            // 通知成员撤回
            if (!record.isReject()) {
                String self = record.getGuid(), target = record.getTarget();
                publisher.pushPermissionEvent(self, target, record.getMid(), PermissionEnum.WITHDRAW);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeMessage(String mid) {
        String userId = ShiroUser.getUserId();
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::getMid, mid);
        wrapper.eq(ChatRecord::getGuid, userId);
        ChatRecord record = getFirst(wrapper);
        Assert.notNull(record, "服务器消息同步中，请稍后再试", IllegalStateException::new);
        // 添加删除标记
        ChatRecordDeleted deleted = new ChatRecordDeleted();
        deleted.setGuid(userId);
        // 确保移除的目标不是自己（自己可能是发起者或目标）
        String guid = record.getGuid(), target = record.getTarget();
        deleted.setTarget(Wss.isGroup(target) || guid.equals(userId) ? target : guid);
        deleted.setRecordId(record.getId());
        deleted.setCreateTime(record.getCreateTime());
        return deletedMapper.insert(deleted) == 1;
    }

    @Override
    public void removeAllMessage(String target) {
        String guid = ShiroUser.getUserId();
        QueryWrapper<ChatRecord> wrapper = Wrappers.query();
        wrapper.select(DbUtil.selectMax(BaseModel::getId));
        LambdaQueryWrapper<ChatRecord> lambda = wrapper.lambda();
        // 群组特殊条件
        if (Wss.isGroup(target)) {
            lambda.eq(ChatRecord::getTarget, target);
        } else {
            // 私聊查找相互发送的消息
            lambda.eq(ChatRecord::getGuid, guid);
            lambda.eq(ChatRecord::getTarget, target);
            lambda.or();
            lambda.eq(ChatRecord::getGuid, target);
            lambda.eq(ChatRecord::getTarget, guid);
        }
        ChatRecord last = getFirst(lambda);
        if (last != null) {
            LambdaUpdateWrapper<ChatRecordOffset> wrapper1 = Wrappers.lambdaUpdate();
            wrapper1.eq(ChatRecordOffset::getGuid, guid);
            wrapper1.eq(ChatRecordOffset::getTarget, target);
            wrapper1.set(ChatRecordOffset::getOffset, last.getId());
            int update = offsetMapper.update(null, wrapper1);
            if (update == 0) {
                ChatRecordOffset offset = new ChatRecordOffset();
                offset.setGuid(guid);
                offset.setTarget(target);
                offset.setOffset(last.getId());
                offsetMapper.insert(offset);
            }
            // 单条消息设置失效
            LambdaUpdateWrapper<ChatRecordDeleted> wrapper2 = Wrappers.lambdaUpdate();
            wrapper2.eq(ChatRecordDeleted::getTarget, target);
            wrapper2.set(BaseModel::isDeleted, 1);
            deletedMapper.update(null, wrapper2);
        }
    }

    @Override
    public void readMessage(String mid, String target) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::isUnread, true);
        // 此方法由阅读者调用，目标为此消息的发起者，匹配对应数据库的uid发起者
        wrapper.eq(ChatRecord::getGuid, target);
        wrapper.eq(ChatRecord::getMid, mid);
        wrapper.eq(ChatRecord::getTarget, ShiroUser.getUserId());
        wrapper.set(ChatRecord::isUnread, 0);
        super.update(wrapper);
    }

    @Override
    public void readAllMessage(String guid, String target, boolean audio) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::isUnread, true);
        // 此方法由阅读者调用，目标为此消息的发起者，匹配对应数据库的uid发起者
        wrapper.eq(ChatRecord::getGuid, target);
        wrapper.eq(ChatRecord::getTarget, guid);
        // 语音消息已读设置
        wrapper.ne(!audio, ChatRecord::getType, CommandEnum.AUDIO.toString());
        wrapper.set(ChatRecord::isUnread, 0);
        super.update(wrapper);
        // 清空redis计数器
        redisManager.setUnreadCount(guid, target, 0);
    }

    @Override
    public Map<String, ChatRecord> getLatestUnreadMessages(String guid) {
        LambdaQueryWrapper<ChatRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecord::getTarget, guid);
        wrapper.eq(ChatRecord::isUnread, 1);
        List<ChatRecord> list = list(wrapper);
        Map<String, ChatRecord> latest = new HashMap<>();
        for (ChatRecord record : list) {
            String muid = record.getGuid();
            ChatRecord last = latest.get(muid);
            // 若没有这个人的消息 || 当前消息比未读消息新
            if (last == null || record.getCreateTime().after(last.getCreateTime())) {
                latest.put(muid, record);
            }
        }
        return latest;
    }
}
