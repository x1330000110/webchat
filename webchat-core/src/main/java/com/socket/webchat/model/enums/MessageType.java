package com.socket.webchat.model.enums;

import cn.hutool.core.lang.EnumItem;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * ws命令操作枚举
 */
@Getter
public enum MessageType implements EnumItem<MessageType>, IEnum<String> {
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
     * 屏蔽
     */
    SHIELD,
    /**
     * 撤回/删除消息
     */
    REMOVE,
    /**
     * 禁言
     */
    MUTE,
    /**
     * 禁止登录
     */
    LOCK,
    /**
     * 设为/取消管理员权限
     */
    ROLE,
    /**
     * 设置头衔
     */
    ALIAS,
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
     * 公告
     */
    ANNOUNCE,
    /**
     * 系统通知等级：主要的
     */
    PRIMARY,
    /**
     * 系统通知等级：成功
     */
    SUCCESS,
    /**
     * 系统通知等级：警告
     */
    WARNING,
    /**
     * 系统通知等级：错误
     */
    DANGER,
    /**
     * 系统通知等级：信息
     */
    INFO,

    /**
     * WebRTC会话标识
     */
    OFFER,
    ANSWER,
    CANDIDATE,
    LEAVE,
    /**
     * 消息已读标记
     */
    READ;

    private final String name;
    private String preview;

    MessageType() {
        this.name = name().toLowerCase();
    }

    MessageType(String preview) {
        this();
        this.preview = preview;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int intVal() {
        return ordinal();
    }

    @Override
    public String getValue() {
        return name;
    }
}
