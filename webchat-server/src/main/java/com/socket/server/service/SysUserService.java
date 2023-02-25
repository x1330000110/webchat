package com.socket.server.service;

import com.socket.core.model.condition.EmailCondition;
import com.socket.core.model.condition.LoginCondition;
import com.socket.core.model.condition.PasswordCondition;
import com.socket.core.model.condition.RegisterCondition;
import com.socket.core.model.enums.UserRole;
import com.socket.core.model.po.SysUser;
import com.socket.core.service.BaseService;

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
     * 内部注册方法（此方法没有任何验证，请勿向外部公开API）
     *
     * @param condition 注册信息
     * @return 用户
     */
    SysUser _register(RegisterCondition condition);

    /**
     * 发送邮箱验证码通用接口
     *
     * @param email guid/邮箱
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
     * @param guid 用户id
     * @return {@link SysUser}
     */
    SysUser getUserInfo(String guid);

    /**
     * 切换目标用户管理员身份
     *
     * @param target 用户
     * @return 修改后的权限
     */
    UserRole switchRole(String target);

    /**
     * 设置指定用户头衔
     *
     * @param target 用户uid
     * @param alias  头衔
     */
    void updateAlias(String target, String alias);
}
