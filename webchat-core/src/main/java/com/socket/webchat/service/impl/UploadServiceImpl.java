package com.socket.webchat.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.exception.UploadException;
import com.socket.webchat.mapper.ChatRecordFileMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FileType;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.request.BaiduSpeechRequest;
import com.socket.webchat.request.LanzouCloudRequest;
import com.socket.webchat.service.UploadService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.RedisClient;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.support.collections.RedisMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl extends ServiceImpl<ChatRecordFileMapper, ChatRecordFile> implements UploadService {
    private final BaiduSpeechRequest baiduSpeechRequest;
    private final ChatRecordMapper chatRecordMapper;
    private final LanzouCloudRequest lanzouRequest;
    private RedisMap<String, String> urlMapping;

    @Autowired
    public void setUrlMapping(RedisClient<String> client) {
        this.urlMapping = client.withMap(RedisTree.LANZOU_URL.get());
    }

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
        String hash = lanzouRequest.generateHash(bytes);
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getHash, hash);
        ChatRecordFile file = getFirst(wrapper);
        // 文件存在则关联mid，不存在获取url后关联
        String url = file != null ? file.getUrl() : lanzouRequest.upload(type, bytes, hash);
        this.save(new ChatRecordFile(condition.getMid(), type, url, hash, size));
        return getMapping(type, hash);
    }

    @Override
    public String getResourceURL(String mid) {
        // 获取消息文件
        LambdaQueryWrapper<ChatRecordFile> wrapper1 = Wrappers.lambdaQuery();
        wrapper1.eq(ChatRecordFile::getMid, mid);
        ChatRecordFile file = getFirst(wrapper1);
        // 检查记录
        if (file == null) {
            return null;
        }
        // 查询消息发起与类型
        LambdaQueryWrapper<ChatRecord> wrapper2 = Wrappers.lambdaQuery();
        wrapper2.eq(ChatRecord::getMid, mid);
        ChatRecord record = chatRecordMapper.selectOne(wrapper2);
        Assert.notNull(record, "正在同步远程消息", IllegalStateException::new);
        // 检查来源
        if (Wss.checkMessagePermission(record)) {
            String url = file.getUrl();
            return urlMapping.computeIfAbsent(url, e -> lanzouRequest.getResourceURL(url));
        }
        return null;
    }

    @Override
    public String getResourceURL(FileType type, String hash) {
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getType, type);
        wrapper.eq(ChatRecordFile::getHash, hash);
        ChatRecordFile file = getFirst(wrapper);
        if (file == null) {
            return null;
        }
        String url = file.getUrl();
        return urlMapping.computeIfAbsent(url, e -> lanzouRequest.getResourceURL(url));
    }

    @Override
    public String convertText(String mid) {
        byte[] bytes = lanzouRequest.download(getResourceURL(mid));
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        return baiduSpeechRequest.convertText(bytes);
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void clearResourceUrl() {
        int size = urlMapping.size();
        // 总数超过100 随机删除一半的数据
        if (size > 100) {
            int clear = size / 2;
            IntStream.range(0, clear).forEach(i -> urlMapping.remove(urlMapping.randomKey()));
            log.info("定时清理urlMapping成功：目前缓存{}条数据", size - clear);
        }
    }
}
