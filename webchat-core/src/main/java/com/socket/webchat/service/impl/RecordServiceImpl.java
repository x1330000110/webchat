package com.socket.webchat.service.impl;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisClient;
import com.socket.webchat.mapper.ChatRecordDeletedMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.mapper.ChatRecordOffsetMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordDeleted;
import com.socket.webchat.model.ChatRecordOffset;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements RecordService {
    private final ChatRecordDeletedMapper chatRecordDeletedMapper;
    private final ChatRecordOffsetMapper chatRecordOffsetMapper;
    private final RedisClient<?> redisClient;

    @KafkaListener(topics = Constants.KAFKA_RECORD)
    public void saveRecord(ConsumerRecord<String, String> data) {
        ChatRecord record = JSONUtil.toBean(data.value(), ChatRecord.class);
        super.save(record);
    }

    @Override
    public List<ChatRecord> getRecords(String mid, String target) {
        String userId = Wss.getUserId();
        // wrapper构造器
        LambdaQueryWrapper<ChatRecord> wrapper = Wss.lambdaQuery();
        wrapper.eq(ChatRecord::isSysmsg, 0);
        // 若查询群组，获取所有目标为群组的消息
        if (Constants.GROUP.equals(target)) {
            wrapper.eq(ChatRecord::getTarget, Constants.GROUP);
        } else {
            // 反之仅获取相互发送的消息
            wrapper.and(ew -> ew.eq(ChatRecord::isReject, false).or().eq(ChatRecord::getUid, userId));
            wrapper.and(ew -> ew
                    .eq(ChatRecord::getUid, userId)
                    .eq(ChatRecord::getTarget, target)
                    .or()
                    .eq(ChatRecord::getUid, target)
                    .eq(ChatRecord::getTarget, userId)
            );
        }
        // 限制起始边界id
        LambdaQueryWrapper<ChatRecordOffset> w1 = Wrappers.lambdaQuery();
        w1.eq(ChatRecordOffset::getUid, userId);
        w1.eq(ChatRecordOffset::getTarget, target);
        ChatRecordOffset limit = chatRecordOffsetMapper.selectOne(w1);
        Opt.ofNullable(limit).ifPresent(e -> wrapper.gt(BaseModel::getId, e.getOffset()));
        // 限制结束边界id
        ChatRecord offset = null;
        if (mid != null) {
            // 通过mid查询id
            LambdaQueryWrapper<ChatRecord> w2 = Wrappers.lambdaQuery();
            w2.eq(ChatRecord::getMid, mid);
            offset = getOne(w2);
            Assert.notNull(offset, "无效的MID", IllegalStateException::new);
            wrapper.lt(BaseModel::getId, offset.getId());
        }
        // 排除已删除的消息id
        LambdaQueryWrapper<ChatRecordDeleted> w3 = Wss.lambdaQuery();
        w3.eq(ChatRecordDeleted::getUid, userId);
        Optional.ofNullable(offset).ifPresent(m -> w3.lt(ChatRecordDeleted::getRecordTime, m.getCreateTime()));
        List<String> deleted = chatRecordDeletedMapper.selectList(w3)
                .stream()
                .map(ChatRecordDeleted::getMid)
                .collect(Collectors.toList());
        wrapper.notIn(!deleted.isEmpty(), ChatRecord::getMid, deleted);
        // 限制结果
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        wrapper.last(StrUtil.format("LIMIT {}", Constants.SYNC_RECORDS_NUMS));
        return list(wrapper);
    }

    @Override
    public ChatRecord withdrawMessage(String uid, String mid) {
        LambdaQueryWrapper<ChatRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecord::getMid, mid);
        wrapper.eq(ChatRecord::getUid, uid);
        // 未送达的消息无撤回时间限制
        wrapper.and(ew -> {
            String condition = StrUtil.format("NOW() - INTERVAL {} SECOND", Constants.WITHDRAW_MESSAGE_TIME);
            String column = Wss.columnToString(ChatRecord::getCreateTime);
            ew.eq(ChatRecord::isReject, true).or().getExpression().add(() -> column, SqlKeyword.GE, () -> condition);
        });
        ChatRecord record = getOne(wrapper);
        if (record != null) {
            record.setDeleted(true);
            super.updateById(record);
            return record;
        }
        return null;
    }

    @Override
    public void removeAllMessage(String uid, String target) {
        QueryWrapper<ChatRecord> wrapper = Wrappers.query();
        wrapper.select(Wss.selecterMax(BaseModel::getId));
        LambdaQueryWrapper<ChatRecord> lambda = wrapper.lambda();
        // 群组特殊条件
        if (Constants.GROUP.equals(target)) {
            lambda.eq(ChatRecord::getTarget, target);
        } else {
            // 私聊查找相互发送的消息
            lambda.eq(ChatRecord::getUid, uid);
            lambda.eq(ChatRecord::getTarget, target);
            lambda.or();
            lambda.eq(ChatRecord::getUid, target);
            lambda.eq(ChatRecord::getTarget, uid);
        }
        ChatRecord last = super.getOne(lambda);
        if (last != null) {
            LambdaUpdateWrapper<ChatRecordOffset> w1 = Wrappers.lambdaUpdate();
            w1.eq(ChatRecordOffset::getUid, uid);
            w1.eq(ChatRecordOffset::getTarget, target);
            w1.set(ChatRecordOffset::getOffset, last.getId());
            int update = chatRecordOffsetMapper.update(null, w1);
            if (update == 0) {
                ChatRecordOffset offset = new ChatRecordOffset();
                offset.setUid(uid);
                offset.setTarget(target);
                offset.setOffset(last.getId());
                chatRecordOffsetMapper.insert(offset);
            }
            // 单条消息设置失效
            LambdaUpdateWrapper<ChatRecordDeleted> w2 = Wss.lambdaUpdate();
            w2.eq(ChatRecordDeleted::getTarget, target);
            w2.set(BaseModel::isDeleted, 1);
            chatRecordDeletedMapper.update(null, w2);
        }
    }

    @Override
    public void readMessage(String mid, String target) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::isUnread, true);
        // 此方法由阅读者调用，目标为此消息的发起者，匹配对应数据库的uid发起者
        wrapper.eq(ChatRecord::getUid, target);
        wrapper.eq(ChatRecord::getMid, mid);
        wrapper.eq(ChatRecord::getTarget, Wss.getUserId());
        wrapper.set(ChatRecord::isUnread, false);
        super.update(wrapper);
    }

    @Override
    public void readAllMessage(String uid, String target, boolean audio) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::isUnread, true);
        // 此方法由阅读者调用，目标为此消息的发起者，匹配对应数据库的uid发起者
        wrapper.eq(ChatRecord::getUid, target);
        wrapper.eq(ChatRecord::getTarget, uid);
        // 语音消息已读设置
        wrapper.ne(!audio, ChatRecord::getType, MessageType.AUDIO.getName());
        wrapper.set(ChatRecord::isUnread, false);
        super.update(wrapper);
        // 清空redis计数器
        redisClient.remove(RedisTree.UNREAD.concat(uid));
    }

    @Override
    public Map<String, SortedSet<ChatRecord>> getUnreadMessages(String uid) {
        Map<String, SortedSet<ChatRecord>> mss = new HashMap<>();
        LambdaQueryWrapper<ChatRecord> wrapper = Wss.lambdaQuery();
        wrapper.eq(ChatRecord::getTarget, uid);
        wrapper.eq(ChatRecord::isUnread, 1);
        // 分组聊天记录
        List<ChatRecord> list = list(wrapper);
        for (ChatRecord record : list) {
            // 未读消息发起者
            String ruid = record.getUid();
            Set<ChatRecord> records = mss.get(ruid);
            if (records == null) {
                SortedSet<ChatRecord> set = new TreeSet<>();
                set.add(record);
                mss.put(ruid, set);
            } else {
                records.add(record);
            }
        }
        return mss;
    }

    @Override
    public boolean removeMessageWithSelf(String mid) {
        LambdaUpdateWrapper<ChatRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ChatRecord::getMid, mid);
        ChatRecord record = getOne(wrapper);
        // 检查消息权限
        if (!Wss.checkMessagePermission(record)) {
            return false;
        }
        // 添加删除标记
        ChatRecordDeleted deleted = new ChatRecordDeleted();
        String userId = Wss.getUserId();
        deleted.setUid(userId);
        // 确保移除的目标不是自己（自己可能是发起者或目标）
        String uid = record.getUid(), target = record.getTarget();
        deleted.setTarget(Constants.GROUP.equals(target) ? Constants.GROUP : uid.equals(userId) ? target : uid);
        deleted.setMid(record.getMid());
        deleted.setRecordTime(record.getCreateTime());
        return chatRecordDeletedMapper.insert(deleted) == 1;
    }
}
