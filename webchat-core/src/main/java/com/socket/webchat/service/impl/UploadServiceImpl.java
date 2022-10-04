package com.socket.webchat.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.FTPClient;
import com.socket.webchat.exception.UploadException;
import com.socket.webchat.mapper.ChatRecordFileMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.FTPFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FilePath;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.request.BaiduSpeechRequest;
import com.socket.webchat.service.UploadService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl extends ServiceImpl<ChatRecordFileMapper, ChatRecordFile> implements UploadService {
    private final BaiduSpeechRequest baiduSpeechRequest;
    private final ChatRecordMapper chatRecordMapper;
    private final FTPClient client;

    @Override
    public String upload(FileCondition condition, FilePath path) throws IOException {
        // 检查散列
        String digest = condition.getDigest();
        if (StrUtil.isNotEmpty(digest)) {
            Assert.isTrue(client.existFile(path, digest), "NOT FOUND", UploadException::new);
            // 查找文件
            FTPFile cache = new FTPFile(path, digest);
            LambdaQueryWrapper<ChatRecordFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatRecordFile::getHash, digest);
            wrapper.last("LIMIT 1");
            // 转存文件
            ChatRecordFile file = getOne(wrapper);
            super.save(new ChatRecordFile(condition.getMid(), cache, file.getSize()));
            return cache.getMapping();
        }
        // 上传文件
        MultipartFile blob = condition.getBlob();
        long size = blob.getSize();
        Assert.isTrue(size != 0, "无效的文件", UploadException::new);
        Assert.isTrue(size < path.getSize(), "文件大小超过限制", UploadException::new);
        // 上传文件获取路径
        FTPFile file = client.upload(path, blob.getBytes());
        // 记录文件
        super.save(new ChatRecordFile(condition.getMid(), file, size));
        return file.getMapping();
    }

    @Override
    public <T extends OutputStream> T writeStream(String mid, T stream) {
        // 获取消息文件
        LambdaQueryWrapper<ChatRecordFile> w1 = Wrappers.lambdaQuery();
        w1.eq(ChatRecordFile::getMid, mid);
        w1.eq(BaseModel::isDeleted, 0);
        ChatRecordFile file = getOne(w1);
        // 检查记录
        if (file == null) {
            return null;
        }
        // 查询消息发起与类型
        LambdaQueryWrapper<ChatRecord> w2 = Wrappers.lambdaQuery();
        w2.eq(ChatRecord::getMid, mid);
        w2.eq(BaseModel::isDeleted, 0);
        ChatRecord record = chatRecordMapper.selectOne(w2);
        // 检查来源
        if (Wss.checkMessagePermission(record)) {
            // 检查文件类型过期时间
            LocalDateTime create = LocalDateTimeUtil.of(file.getCreateTime());
            MessageType type = record.getType();
            if (type != MessageType.BLOB || create.plusDays(Constants.FILE_EXPIRED_DAYS).isAfter(LocalDateTime.now())) {
                return writeStream(FilePath.of(type), file.getHash(), stream);
            }
        }
        return null;
    }

    @Override
    public <T extends OutputStream> T writeStream(FilePath path, String hash, T stream) {
        return client.download(path, hash, stream);
    }

    @Override
    public String convertText(String mid) {
        ByteArrayOutputStream stream = this.writeStream(mid, new ByteArrayOutputStream());
        byte[] bytes = stream.toByteArray();
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        return baiduSpeechRequest.convertText(bytes);
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * ?")
    public void clearExpiredResources() {
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ChatRecordFile::getType, FilePath.BLOB);
        List<ChatRecordFile> list = list(wrapper);
        LocalDateTime offset = LocalDate.now().atStartOfDay().minusDays(Constants.FILE_EXPIRED_DAYS);
        // 获取没有过期的文件hash
        List<String> noexpired = list.stream()
                .filter(e -> LocalDateTimeUtil.of(e.getCreateTime()).isAfter(offset))
                .map(ChatRecordFile::getHash)
                .collect(Collectors.toList());
        // 获取过期FTP文件
        Map<String, String> collect = list.stream()
                .filter(e -> LocalDateTimeUtil.of(e.getCreateTime()).isBefore(offset))
                .filter(e -> !noexpired.contains(e.getHash()))
                .collect(Collectors.toMap(ChatRecordFile::getHash, ChatRecordFile::getPath, (a, b) -> a));
        // 删除过期文件
        client.deleteFiles(collect);
    }
}
