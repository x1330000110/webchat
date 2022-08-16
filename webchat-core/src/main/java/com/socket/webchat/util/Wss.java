package com.socket.webchat.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.useragent.Platform;
import cn.hutool.http.useragent.UserAgentParser;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysUser;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Wss {
    private static final String REGEX = "地址\\s+:\\s+(.+?)\\s+(.+?)\\s+(.+?)\n";

    /**
     * 将微信openId转为wsUid
     */
    public static String toUID(String openid) {
        String digest = MD5.create().digestHex(openid);
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, digest.length()).filter(i -> i % 2 == 0).forEach(i -> sb.append(digest.charAt(i)));
        return String.valueOf(Long.parseLong(sb.substring(0, 6), 16) & 1000000);
    }

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
     * 获取指定函数式接口命名形式（核心实现由Mybatis-Plus支持）
     */
    public static <T> String columnToString(SFunction<T, ?> lambda) {
        return StrUtil.toUnderlineCase(PropertyNamer.methodToProperty(LambdaUtils.resolve(lambda).getImplMethodName()));
    }

    /**
     * Get spring managed beans
     */
    public static <T> T getBean(Class<T> clazz) {
        WebApplicationContext context = WebApplicationContextUtils.findWebApplicationContext(Sessions.get().getServletContext());
        return Opt.ofNullable(context).map(c -> c.getBean(clazz)).get();
    }

    /**
     * Get the domain name of the current host
     */
    public static String getHost() {
        HttpServletRequest request = Requests.get();
        StringBuilder url = new StringBuilder();
        String scheme = request.getScheme();
        int port = request.getServerPort();
        if (port < 0) {
            port = 80;
        }
        url.append(scheme);
        url.append("://");
        url.append(request.getServerName());
        if (port != 80 && port != 443) {
            url.append(':');
            url.append(port);
        }
        return url.toString();
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
     * Get the real IP address of the client
     */
    public static String getRemoteIP() {
        return ServletUtil.getClientIP(Requests.get());
    }

    /**
     * Get the province where the specified IP address is located
     */
    public static String getProvince(String ip) {
        String body = HttpRequest.get("http://www.cip.cc/".concat(ip)).execute().body();
        List<String> all = ReUtil.findAll(Pattern.compile(REGEX), body, 2);
        return all.get(0);
    }

    /**
     * Get the current device login platform
     */
    public static String getPlatform(String userAgent) {
        Platform platform = UserAgentParser.parse(userAgent).getPlatform();
        return platform.isAndroid() ? "手机在线" : platform.isIos() ? "iPhone在线" : "PC在线";
    }

    /**
     * 检查消息是否有操作权限（目标是群组，发起者是自己或目标是自己）
     */
    public static boolean checkMessagePermissions(ChatRecord record) {
        String userId = getUserId();
        if (userId == null) {
            return false;
        }
        return Constants.GROUP.equals(record.getTarget()) || userId.equals(record.getUid()) || userId.equals(record.getTarget());
    }
}
