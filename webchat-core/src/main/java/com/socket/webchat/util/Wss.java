package com.socket.webchat.util;

import cn.hutool.crypto.SecureUtil;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.ChatRecord;

public class Wss {

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
     * 检查消息是否有操作权限（目标是群组，发起者是自己或目标是自己）
     */
    public static boolean checkMessagePermission(ChatRecord record) {
        String userId = ShiroUser.getUserId();
        if (userId == null) {
            return false;
        }
        boolean isgroup = Wss.isGroup(record.getTarget());
        boolean self = userId.equals(record.getGuid());
        boolean target = userId.equals(record.getTarget());
        return isgroup || self || target;
    }

    /**
     * 检查目标是否为群组
     */
    public static boolean isGroup(String guid) {
        return guid != null && (Constants.DEFAULT_GROUP.equals(guid) || guid.startsWith(Constants.GROUP_PREFIX));
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
