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
     * 登录平台
     */
    private String platform;
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
    private Integer unreads;

    public UserPreview(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    public void fill(WsUser wsUser) {
        if (wsUser != null) {
            this.online = true;
            this.platform = wsUser.getPlatform();
        }
    }
}
