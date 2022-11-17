package com.socket.webchat.custom;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.exception.AccountException;
import com.socket.webchat.exception.OffsiteLoginException;
import com.socket.webchat.mapper.SysUserLogMapper;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.BaseModel;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.enums.RedisTree;
import com.socket.webchat.request.IPRequest;
import com.socket.webchat.util.Bcrypt;
import com.socket.webchat.util.RedisClient;
import com.socket.webchat.util.Requests;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Shiro登录验证与权限检查
 */
@Component
@RequiredArgsConstructor
public class CustomRealm extends AuthorizingRealm {
    private final SysUserLogMapper sysUserLogMapper;
    private final SysUserMapper sysUserMapper;
    private final IPRequest ipRequest;
    private final RedisClient<?> redis;

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
        String uid = (String) token.getPrincipal();
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(uid.contains("@") ? SysUser::getEmail : SysUser::getUid, uid);
        SysUser user = sysUserMapper.selectOne(wrapper);
        // 无效账号
        if (user == null) {
            return null;
        }
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
                SysUser user = info.getPrincipals().oneByType(SysUser.class);
                String input = new String((char[]) token.getCredentials());
                // 微信默认密码登录
                if (StrUtil.equals(input, Constants.WX_DEFAULT_PASSWORD)) {
                    checkLimit(user.getUid());
                    return true;
                }
                // 验证密码
                if (Bcrypt.verify(input, (String) info.getCredentials())) {
                    checkLimit(user.getUid());
                    checkOffsite(user);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 异地登录检查
     */
    private void checkOffsite(SysUser user) {
        // 未绑定邮箱
        if (StrUtil.isEmpty(user.getEmail())) {
            return;
        }
        // 查询登录记录
        LambdaQueryWrapper<SysUserLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUserLog::getUid, user.getUid());
        wrapper.orderByDesc(BaseModel::getCreateTime);
        wrapper.last("LIMIT 1");
        SysUserLog log = sysUserLogMapper.selectOne(wrapper);
        // 首次登录放行
        if (log == null) {
            return;
        }
        // 检查标记
        if (Requests.notExist(Constants.OFFSITE)) {
            // 检查异地
            String remoteIP = Wss.getRemoteIP();
            if (!Objects.equals(log.getIp(), remoteIP)) {
                // IP所属省是否相同
                boolean offsite = Objects.equals(log.getRemoteProvince(), ipRequest.getProvince(remoteIP));
                // 返回的异常为脱敏的绑定邮箱信息
                Assert.isTrue(offsite, DesensitizedUtil.email(user.getEmail()), OffsiteLoginException::new);
            }
        }
    }

    /**
     * 验证登录限制
     */
    private void checkLimit(String uid) {
        long time = redis.getExpired(RedisTree.LOCK.concat(uid));
        if (time > 0) {
            throw new AccountException("您已被限制登录，预计剩余" + Wss.universal(time));
        }
    }
}
