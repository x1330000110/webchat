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
     * 创建游客
     */
    public static SysUser newGuest() {
        String uid = RandomUtil.randomNumbers(6);
        return new SysUser().setRole(UserRole.GUEST).setUid("GUEST_" + uid).setName("游客" + uid);
    }
}
