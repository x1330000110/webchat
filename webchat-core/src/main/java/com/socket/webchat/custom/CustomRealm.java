package com.socket.webchat.custom;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.runtime.AccountException;
import com.socket.webchat.runtime.OffsiteLoginException;
import com.socket.webchat.util.*;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Shiro登录验证与权限检查
 */
@Component
public class CustomRealm extends AuthorizingRealm {
    private RedisTemplate<String, Integer> template;
    private SysUserMapper sysUserMapper;

    @Autowired
    public void setSysUserMapper(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Autowired
    public void setRedisWrapper(RedisTemplate<String, Integer> template) {
        this.template = template;
    }

    /**
     * 权限认证
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUid, principals.getPrimaryPrincipal());
        Optional.of(sysUserMapper.selectOne(wrapper)).ifPresent(e -> info.addRole(e.getRole().toString()));
        return info;
    }

    /**
     * 账号认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        String uid = (String) token.getPrincipal();
        wrapper.eq(uid.contains("@") ? SysUser::getEmail : SysUser::getUid, uid);
        SysUser user = sysUserMapper.selectOne(wrapper);
        // 无效账号
        if (user == null) {
            return null;
        }
        // 限制登录检查
        Assert.isFalse(user.isDeleted(), "该账号已被永久限制登录", AccountException::new);
        // 验证密码
        return new SimpleAuthenticationInfo(user, user.getHash(), super.getName());
    }

    /**
     * 密码认证（Bcrypt认证）
     */
    @Override
    public void setCredentialsMatcher(CredentialsMatcher credentialsMatcher) {
        super.setCredentialsMatcher(new SimpleCredentialsMatcher() {
            @Override
            public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
                String input = new String((char[]) token.getCredentials());
                SysUser user = info.getPrincipals().oneByType(SysUser.class);
                // 默认微信登录
                if (StrUtil.equals(input, Constants.WX_DEFAULT_PASSWORD)) {
                    checkLimit(user.getUid());
                    return true;
                }
                // 验证密码
                if (Bcrypt.verify(input, (String) info.getCredentials())) {
                    checkLimit(user.getUid());
                    return checkOffsite(user);
                }
                return false;
            }
        });
    }

    /**
     * 异地登录检查
     */
    private boolean checkOffsite(SysUser sysUser) {
        // 未绑定邮箱或首次登录不会检查
        if (StrUtil.isEmpty(sysUser.getEmail()) || StrUtil.isEmpty(sysUser.getIp())) {
            return true;
        }
        // 检查标记
        if (Requests.notExist(Constants.OFFSITE)) {
            // 检查异地
            String remoteip = Wss.getRemoteIP();
            String lastip = sysUser.getIp();
            if (!Objects.equals(lastip, remoteip)) {
                // IP所属省是否相同
                boolean offsite = Objects.equals(Wss.getProvince(remoteip), Wss.getProvince(lastip));
                // 返回的异常为脱敏的绑定邮箱信息
                Assert.isTrue(offsite, DesensitizedUtil.email(sysUser.getEmail()), OffsiteLoginException::new);
            }
        }
        return true;
    }

    /**
     * 验证登录限制
     */
    private void checkLimit(String uid) {
        long time = RedisValue.of(template, RedisTree.LOCK.concat(uid)).getExpired();
        Assert.isTrue(time <= 0, () -> new AccountException("您已被限制登录，预计剩余" + Wss.universal(time)));
    }
}
