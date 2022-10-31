package com.socket.webchat.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.exception.UploadException;
import com.socket.webchat.mapper.ChatRecordFileMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FileType;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.request.BaiduSpeechRequest;
import com.socket.webchat.request.LanzouCloudRequest;
import com.socket.webchat.service.UploadService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl extends ServiceImpl<ChatRecordFileMapper, ChatRecordFile> implements UploadService {
    private final BaiduSpeechRequest baiduSpeechRequest;
    private final LanzouCloudRequest lanzouRequest;
    private final ChatRecordMapper chatRecordMapper;

    @Override
    public String upload(FileCondition condition, FileType type) throws IOException {
        // 检查散列
        String digest = condition.getDigest();
        if (StrUtil.isNotEmpty(digest)) {
            LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(ChatRecordFile::getHash, digest);
            ChatRecordFile file = getOne(wrapper);
            if (file != null) {
                return file.getUrl();
            }
            throw new IllegalStateException("NOT FOUND FILE");
        }
        // 上传文件
        MultipartFile blob = condition.getBlob();
        long size = blob.getSize();
        Assert.isTrue(size != 0, "无效的文件", UploadException::new);
        Assert.isTrue(size < type.getSize(), "文件大小超过限制", UploadException::new);
        // 上传文件获取路径
        byte[] bytes = blob.getBytes();
        String hash = lanzouRequest.generateHash(bytes);
        String url = lanzouRequest.upload(type, bytes, hash);
        // 记录文件
        super.save(new ChatRecordFile(condition.getMid(), type, url, hash, size));
        return url;
    }

    @Override
    public String getResourceURL(String mid) {
        // 获取消息文件
        LambdaQueryWrapper<ChatRecordFile> wrapper1 = Wrappers.lambdaQuery();
        wrapper1.eq(ChatRecordFile::getMid, mid);
        ChatRecordFile file = getOne(wrapper1);
        // 检查记录
        if (file == null) {
            return null;
        }
        // 查询消息发起与类型
        LambdaQueryWrapper<ChatRecord> wrapper2 = Wrappers.lambdaQuery();
        wrapper2.eq(ChatRecord::getMid, mid);
        ChatRecord record = chatRecordMapper.selectOne(wrapper2);
        // 检查来源
        if (Wss.checkMessagePermission(record)) {
            // 检查文件类型过期时间
            LocalDateTime create = LocalDateTimeUtil.of(file.getCreateTime());
            MessageType type = record.getType();
            if (type != MessageType.BLOB || create.plusDays(Constants.FILE_EXPIRED_DAYS).isAfter(LocalDateTime.now())) {
                return lanzouRequest.getResourceURL(file.getUrl());
            }
        }
        return null;
    }

    @Override
    public String getResourceURL(FileType type, String hash) {
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getType, type);
        wrapper.eq(ChatRecordFile::getHash, hash);
        ChatRecordFile file = getOne(wrapper);
        if (file == null) {
            return null;
        }
        return lanzouRequest.getResourceURL(file.getUrl());
    }

    @Override
    public String convertText(String mid) {
        byte[] bytes = lanzouRequest.download(mid);
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        return baiduSpeechRequest.convertText(bytes);
    }
}
