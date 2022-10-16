package com.socket.webchat.model;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.socket.webchat.model.enums.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户信息
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SysUser extends BaseModel implements Serializable {
    /**
     * 昵称
     */
    private String name;
    /**
     * 账号/用户ID
     */
    @EqualsAndHashCode.Include
    private String uid;
    /**
     * 散列密码
     */
    @JsonIgnore
    @PropIgnore
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
     * 微信openid
     */
    @JsonIgnore
    private String openid;
    /**
     * 手机
     */
    private String phone;
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
     * 头衔
     */
    private String alias;

    /**
     * 创建用户
     */
    public static SysUser newUser() {
        return new SysUser().setRole(UserRole.USER).setUid(RandomUtil.randomNumbers(6));
    }

    /**
     * 当前登录的用户是否是管理员
     */
    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN || isOwner();
    }

    /**
     * 检查当前用户是否是所有者
     */
    public boolean isOwner() {
        return getRole() == UserRole.OWNER;
    }

    @PropIgnore
    public String getOpenid() {
        return openid;
    }
}
