package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import com.socket.webchat.model.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 初始化聊天室的额外用户信息
 *
 * @date 2021/8/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPreview extends SysUser {
    /**
     * 屏蔽的用户UID列表
     */
    private List<String> shields;
    /**
     * 是否在线
     */
    private boolean online;
    /**
     * 未读的最后一条消息
     */
    private String preview;
    /**
     * 未读消息发送时间
     */
    private Long lastTime;
    /**
     * 未读消息总数
     */
    private Integer unreadCount;

    public UserPreview(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    /**
     * 脱敏信息处理
     */
    public void desensit() {
        setIp(null);
        setHash(null);
        this.preview = online ? preview : null;
    }
}
