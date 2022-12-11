package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import com.socket.client.model.enums.OnlineState;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.command.impl.CommandEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览用户
 *
 * @date 2021/8/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserPreview extends SysUser {
    /**
     * 屏蔽的用户UID列表
     */
    private List<String> shields;
    /**
     * 是否在线
     */
    private OnlineState online;
    /**
     * 登录平台
     */
    private String platform;
    /**
     * IP所属省
     */
    private String remoteProvince;
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

    public UserPreview(SysUser source) {
        BeanUtil.copyProperties(source, this);
    }

    public void setPreview(ChatRecord unread) {
        CommandEnum type = CommandEnum.of(unread.getType());
        this.preview = type == CommandEnum.TEXT ? unread.getContent() : '[' + type.getPreview() + ']';
    }
}
