package com.socket.webchat.service;

import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.EmailCondition;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.model.condition.PasswordCondition;
import com.socket.webchat.model.condition.RegisterCondition;

public interface SysUserService extends BaseService<SysUser> {
    /**
     * 登录（如果没有抛出异常，则登录成功）
     *
     * @param condition 用户提交的信息
     */
    void login(LoginCondition condition);

    /**
     * 注册（如果没有抛出异常，则注册成功）
     *
     * @param condition 用户提交的信息
     */
    void register(RegisterCondition condition);

    /**
     * 发送邮箱验证码通用接口
     *
     * @param email uid/邮箱
     * @return 邮箱信息
     */
    String sendEmail(String email);

    /**
     * 修改密码（只能修改自己）
     *
     * @param condition 密码
     * @return 是否成功
     */
    boolean updatePassword(PasswordCondition condition);

    /**
     * 更新用户资料（只能更新自己）
     *
     * @param user 用户信息
     */
    void updateMaterial(SysUser user);

    /**
     * 修改用户头像（只能修改自己）
     *
     * @param bytes 头像数据
     * @return 新的头像URL地址
     */
    String updateAvatar(byte[] bytes);

    /**
     * 修改邮箱
     *
     * @param condition 邮箱参数
     */
    void updateEmail(EmailCondition condition);

    /**
     * 用户个人信息
     *
     * @param uid 用户id
     * @return {@link SysUser}
     */
    SysUser getUserInfo(String uid);
}
