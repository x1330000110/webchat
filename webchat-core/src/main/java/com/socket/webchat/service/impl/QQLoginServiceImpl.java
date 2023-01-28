package com.socket.webchat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.condition.LoginCondition;
import com.socket.webchat.model.condition.RegisterCondition;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.request.QQRequest;
import com.socket.webchat.request.bean.QQAuth;
import com.socket.webchat.request.bean.QQAuthResp;
import com.socket.webchat.request.bean.QQUser;
import com.socket.webchat.service.QQLoginService;
import com.socket.webchat.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QQLoginServiceImpl implements QQLoginService {
    private final SysUserService sysUserService;
    private final QQRequest qqRequest;

    public QQAuth getLoginAuth() {
        return qqRequest.getAuth();
    }

    public HttpStatus state(String qrsig) {
        QQAuthResp verify = qqRequest.verifyAuth(qrsig);
        String state = verify.getState();
        if ("未失效".equals(state) || "认证中".equals(state)) {
            return HttpStatus.WAITTING.body("等待访问");
        }
        if ("已失效".equals(state) || StrUtil.isEmpty(verify.getUin())) {
            return HttpStatus.FAILURE.message("已失效");
        }
        checkLogin(verify.getUin());
        return HttpStatus.SUCCESS.message("登录成功");
    }

    private void checkLogin(String uin) {
        // 查找关联的guid
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUin, uin);
        SysUser user = sysUserService.getFirst(wrapper);
        String guid = Optional.ofNullable(user).map(BaseUser::getGuid).orElseGet(() -> {
            // 注册
            RegisterCondition condition = new RegisterCondition();
            // 获取qq信息
            QQUser info = qqRequest.getInfo(uin.replace("o", ""));
            if (info != null) {
                if (!info.getName().isEmpty()) {
                    condition.setName(StrUtil.sub(info.getName().trim(), 0, 6));
                }
                condition.setImgurl(info.getImgurl());
            }
            condition.setUin(uin);
            condition.setPass(Constants.DEFAULT_PASSWORD);
            return sysUserService._register(condition).getGuid();
        });
        // 登录
        sysUserService.login(new LoginCondition(guid, Constants.DEFAULT_PASSWORD));
    }
}
