package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import com.socket.webchat.model.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 列表预览用户信息
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
    /**
     * 是否为群组
     */
    private boolean group;
    /**
     * 群组成员
     */
    private List<String> members;

    public UserPreview(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    /**
     * 填充访问时间
     *
     * @param logs 日志列表
     */
    public void setLastTime(Map<String, Date> logs) {
        Optional.ofNullable(logs.get(getUid())).ifPresent(date -> this.lastTime = date.getTime());
    }

    public void setLastTime(Date time) {
        this.lastTime = time.getTime();
    }
}
