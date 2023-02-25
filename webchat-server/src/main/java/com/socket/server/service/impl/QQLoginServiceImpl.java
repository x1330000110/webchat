package com.socket.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.core.constant.Constants;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.condition.LoginCondition;
import com.socket.core.model.condition.RegisterCondition;
import com.socket.core.model.enums.HttpStatus;
import com.socket.core.model.po.SysUser;
import com.socket.core.model.request.QQAuthReq;
import com.socket.core.model.request.QQAuthResp;
import com.socket.core.model.request.QQUser;
import com.socket.server.request.QQRequest;
import com.socket.server.service.QQLoginService;
import com.socket.server.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QQLoginServiceImpl implements QQLoginService {
    private final SysUserService sysUserService;
    private final QQRequest qqRequest;

    public QQAuthReq getLoginAuth() {
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
