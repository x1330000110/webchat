package com.socket.webchat.model;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.socket.webchat.model.enums.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 用户登录信息
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseModel implements Serializable {
    /**
     * 昵称
     */
    private String name;
    /**
     * 账号/用户ID
     */
    private String uid;
    /**
     * 散列密码
     */
    @JsonIgnore
    private String hash;
    /**
     * 角色
     */
    private UserRole role;
    /**
     * 绑定邮箱
     */
    private String email;
    /**
     * 手机
     */
    private String phone;
    /**
     * 最近登录ip地址
     */
    @JsonIgnore
    private String ip;
    /**
     * 登录平台
     */
    private String platform;
    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate birth;
    /**
     * 性别（1男 2女）
     */
    private Integer sex;
    /**
     * 年龄
     */
    private Integer age;
    /**
     * 头像地址
     */
    private String headimgurl;
    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date loginTime;
    /**
     * 头衔
     */
    private String alias;

    /**
     * 初始化必要数据
     */
    public static SysUser newInstance() {
        return (SysUser) new SysUser().setRole(UserRole.USER).setUid(RandomUtil.randomNumbers(6)).setCreateTime(new Date());
    }
}
