package com.socket.webchat.model.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * ws命令操作枚举
 */
@Getter
public enum MessageType implements Command<MessageType>, IEnum<String> {
    /**
     * 加载聊天室所有用户
     */
    INIT,
    /**
     * 用户加入
     */
    JOIN,
    /**
     * 用户退出
     */
    EXIT,
    /**
     * 选择用户变动事件
     */
    CHOOSE,
    /**
     * 在线状态变动事件
     */
    CHANGE,
    /**
     * 消息类型：文字
     */
    TEXT,
    /**
     * 消息类型：文件
     */
    BLOB("文件"),
    /**
     * 消息类型：图片
     */
    IMAGE("图片消息"),
    /**
     * 消息类型：语音（系统消息时为语音通话请求）
     */
    AUDIO("语音消息"),
    /**
     * 消息类型：视频（系统消息时为视频通话请求）
     */
    VIDEO("视频文件"),
    /**
     * 消息已读标记
     */
    READ,
    /**
     * 系统通知等级
     */
    PRIMARY,
    SUCCESS,
    WARNING,
    DANGER,
    ERROR,
    /**
     * WebRTC会话标识
     */
    OFFER,
    ANSWER,
    CANDIDATE,
    LEAVE;

    private final String name;
    private String preview;

    MessageType() {
        this.name = name().toLowerCase();
    }

    MessageType(String preview) {
        this();
        this.preview = preview;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name;
    }
}
