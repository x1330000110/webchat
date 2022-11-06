package com.socket.webchat.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.Platform;
import cn.hutool.http.useragent.UserAgentParser;
import cn.hutool.json.JSONObject;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import org.apache.shiro.SecurityUtils;

public class Wss {

    /**
     * 枚举转JSON
     */
    public static <E extends Enum<E>> String toJson(E e) {
        JSONObject json = new JSONObject();
        BeanUtil.descForEach(e.getClass(), prop -> json.set(prop.getFieldName(), prop.getValue(e)));
        return json.toString();
    }

    /**
     * 获取当前用户登录的UID
     */
    public static SysUser getUser() {
        return (SysUser) SecurityUtils.getSubject().getPrincipal();
    }

    /**
     * 获取当前用户登录的UID
     */
    public static String getUserId() {
        return Opt.ofNullable(getUser()).map(SysUser::getUid).get();
    }

    /**
     * 获取指定函数式接口命名形式
     */
    public static <T> String columnToString(Func1<T, ?> lambda) {
        return StrUtil.toUnderlineCase(LambdaUtil.getFieldName(lambda));
    }

    /**
     * 转为通用时间 (1分钟 1小时 1天)
     */
    public static String universal(long second) {
        if (second < 0) throw new IllegalArgumentException("无效的时间：" + second);
        if (second < 60) return second + "秒";
        if (second < 60 * 60) return second / 60 + "分钟";
        long hour = second / (60 * 60);
        if (second < 60 * 60 * 24) return hour + "小时";
        long days = second / (60 * 60 * 24);
        return days + "天" + (hour - days * 24) + "小时";
    }

    /**
     * 获取客户端的真实IP地址
     */
    public static String getRemoteIP() {
        return ServletUtil.getClientIP(Requests.get());
    }

    /**
     * 获取当前请求登录平台
     */
    public static String getPlatform() {
        return getPlatform(Requests.get().getHeader(Header.USER_AGENT.getValue()));
    }

    /**
     * 获取当前请求登录平台
     */
    public static String getPlatform(String userAgent) {
        Platform platform = UserAgentParser.parse(userAgent).getPlatform();
        return platform.isAndroid() ? "手机" : platform.isIos() ? "iPhone" : "PC";
    }

    /**
     * 检查消息是否有操作权限（目标是群组，发起者是自己或目标是自己）
     */
    public static boolean checkMessagePermission(ChatRecord record) {
        String userId = getUserId();
        if (userId == null) {
            return false;
        }
        return Wss.isGroup(record.getTarget()) || userId.equals(record.getUid()) || userId.equals(record.getTarget());
    }


    /**
     * mybatis group by max合成字符串
     */
    public static <T> String selecterMax(Func1<T, ?> lambda) {
        String column = columnToString(lambda);
        return StrUtil.format("MAX({}) AS {}", column, column);
    }

    /**
     * 检查目标是否为群组
     */
    public static boolean isGroup(String target) {
        return target != null && target.startsWith(Constants.GROUP);
    }

    /**
     * 生成数据签名
     *
     * @param bytes 数据
     * @return 签名
     */
    public static String generateHash(byte[] bytes) {
        return SecureUtil.hmacMd5(String.valueOf(bytes.length << bytes.length / 2)).digestHex(bytes) + ".txt";
    }
}
