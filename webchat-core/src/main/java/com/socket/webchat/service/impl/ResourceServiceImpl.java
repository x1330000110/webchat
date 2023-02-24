package com.socket.webchat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.secure.util.Assert;
import com.socket.webchat.custom.storage.ResourceStorage;
import com.socket.webchat.exception.UploadException;
import com.socket.webchat.mapper.ChatRecordFileMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.condition.URLCondition;
import com.socket.webchat.model.enums.FileType;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.model.enums.VideoType;
import com.socket.webchat.request.BaiduSpeechRequest;
import com.socket.webchat.service.ResourceService;
import com.socket.webchat.util.Enums;
import com.socket.webchat.util.RedisClient;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 文件储存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends ServiceImpl<ChatRecordFileMapper, ChatRecordFile> implements ResourceService {
    private final BaiduSpeechRequest speechRequest;
    private final ChatRecordMapper chatRecordMapper;
    private final ResourceStorage resourceStorage;
    private final RedisClient<String> client;

    @Override
    public String upload(FileCondition condition, FileType type) throws IOException {
        // 上传文件
        MultipartFile blob = condition.getBlob();
        long size = blob.getSize();
        Assert.isTrue(size != 0, "无效的文件", UploadException::new);
        Assert.isTrue(size < type.getSize(), "文件大小超过限制", UploadException::new);
        // 获取文件路径
        byte[] bytes = blob.getBytes();
        // 再检查一遍文件hash是否存在
        String hash = Wss.generateHash(bytes);
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getHash, hash);
        wrapper.eq(ChatRecordFile::getType, type.getKey());
        ChatRecordFile file = getFirst(wrapper);
        // 文件存在则关联mid，不存在获取url后关联
        String url = Optional.ofNullable(file)
                .map(ChatRecordFile::getUrl)
                .orElseGet(() -> resourceStorage.upload(type, bytes, hash));
        this.save(new ChatRecordFile(condition.getMid(), type.getKey(), url, hash, size));
        return getMapping(type, hash);
    }

    @Override
    public String convertText(String mid) {
        return Optional.ofNullable(getResourceURL(mid))
                // 下载资源
                .map(resourceStorage::download)
                .filter(bytes -> bytes.length > 0)
                // 转文字
                .map(speechRequest::convertText)
                .orElse(null);
    }

    @Override
    public String getResourceURL(String mid) {
        // 获取消息文件
        LambdaQueryWrapper<ChatRecordFile> wrapper1 = Wrappers.lambdaQuery();
        wrapper1.eq(ChatRecordFile::getMid, mid);
        ChatRecordFile file = getFirst(wrapper1);
        // 检查记录
        if (file != null) {
            // 查询消息发起与类型
            LambdaQueryWrapper<ChatRecord> wrapper2 = Wrappers.lambdaQuery();
            wrapper2.eq(ChatRecord::getMid, mid);
            ChatRecord record = chatRecordMapper.selectOne(wrapper2);
            Assert.notNull(record, "正在同步远程消息", IllegalStateException::new);
            // 检查来源
            if (Wss.checkMessagePermission(record)) {
                String url = file.getUrl();
                VideoType parse = Enums.of(VideoType.class, file.getType());
                // Enum非空 ? 解析请求 : 返回URL
                return parse != null ? parse.parseURL(url) : getOriginalURL(url);
            }
        }
        return null;
    }

    @Override
    public String getResourceURL(FileType type, String hash) {
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getType, type.getKey());
        wrapper.eq(ChatRecordFile::getHash, hash);
        ChatRecordFile file = getFirst(wrapper);
        if (file == null) {
            return null;
        }
        return getOriginalURL(file.getUrl());
    }

    @Override
    public void saveResolve(URLCondition condition) {
        String url = condition.getUrl();
        VideoType parse = Enums.of(VideoType.class, condition.getType());
        Assert.notNull(parse, IllegalArgumentException::new);
        String hash = Wss.generateHash(url.getBytes(StandardCharsets.UTF_8));
        this.save(new ChatRecordFile(condition.getMid(), parse.getKey(), url, hash, null));
    }

    /**
     * 优先从Redis缓存获取URL
     */
    private String getOriginalURL(String url) {
        String key = RedisTree.RESOURCE_URL.concat(url);
        String mapping = client.get(key);
        if (mapping == null) {
            mapping = resourceStorage.getOriginalURL(url);
            if (mapping != null) {
                client.set(key, mapping, 10, TimeUnit.MINUTES);
            }
        }
        return mapping;
    }
}
