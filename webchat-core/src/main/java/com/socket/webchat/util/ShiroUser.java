package com.socket.webchat.util;

import cn.hutool.core.bean.BeanDesc;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.ReflectUtil;
import com.socket.webchat.model.SysUser;
import org.apache.shiro.SecurityUtils;

import java.lang.reflect.Method;

/**
 * Shiro用户管理工具
 */
public class ShiroUser {
    private static final BeanDesc desc;

    static {
        desc = BeanUtil.getBeanDesc(SysUser.class);
    }

    /**
     * 获取当前用户登录的UID
     */
    public static String getUserId() {
        return Opt.ofNullable(get()).map(SysUser::getGuid).get();
    }

    /**
     * 获取当前登录用户
     */
    public static SysUser get() {
        return (SysUser) SecurityUtils.getSubject().getPrincipal();
    }

    /**
     * 设置当前用户属性值
     */
    public static <R> void set(Func1<SysUser, R> getter, R value) {
        String field = LambdaUtil.getFieldName(getter);
        Method setter = desc.getSetter(field);
        ReflectUtil.invoke(get(), setter, value);
    }
}
