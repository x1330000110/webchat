package com.socket.webchat.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.ftp.FTPClient;
import com.socket.webchat.custom.ftp.FTPFile;
import com.socket.webchat.mapper.ChatRecordFileMapper;
import com.socket.webchat.mapper.ChatRecordMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.ChatRecordFile;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FilePath;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.request.BaiduSpeechRequest;
import com.socket.webchat.runtime.UploadException;
import com.socket.webchat.service.UploadService;
import com.socket.webchat.util.Assert;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传服务
 *
 * @date 2021/7/8
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
        if (StrUtil.isNotEmpty(digest) && client.existFile(path, digest)) {
            return new FTPFile(path, digest).getMapping();
        }
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
        // 当前用户UID
        String userId = Wss.getUserId();
        // 检查来源
        if (Constants.GROUP.equals(record.getTarget()) || userId.equals(record.getUid()) || userId.equals(record.getTarget())) {
            // 检查文件类型过期时间
            LocalDateTime create = LocalDateTime.ofInstant(file.getCreateTime().toInstant(), ZoneId.systemDefault());
            MessageType type = record.getType();
            if (type != MessageType.BLOB || create.plusDays(Constants.FILE_EXPIRED_DAYS).isAfter(LocalDateTime.now())) {
                return writeStream(FilePath.of(type), file.getHash(), stream);
            }
        }
        return null;
    }

    @Override
    public <T extends OutputStream> T writeStream(FilePath path, String hash, T stream) {
        return client.download(path.getName(), hash, stream);
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

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearExpiredResources() {
        LambdaQueryWrapper<ChatRecordFile> wrapper = Wrappers.lambdaQuery();
        wrapper.likeLeft(ChatRecordFile::getPath, FilePath.BLOB.getName());
        // 清理4天前的数据（第三天的文件还没有过期）
        int days = Constants.FILE_EXPIRED_DAYS + 1;
        String condition = StrUtil.format("NOW() - INTERVAL {} SECOND", days);
        String column = Wss.columnToString(ChatRecord::getCreateTime);
        wrapper.getExpression().add(() -> column, SqlKeyword.LT, () -> condition);
        List<String> paths = list(wrapper).stream().map(ChatRecordFile::getPath).collect(Collectors.toList());
        client.deleteFiles(paths);
    }
}
