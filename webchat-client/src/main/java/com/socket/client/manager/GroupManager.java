package com.socket.client.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.exception.SocketException;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.custom.listener.GroupChangeEvent;
import com.socket.webchat.custom.listener.GroupChangeLinstener;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ws群组管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupManager extends ConcurrentHashMap<SysGroup, List<String>> implements InitializingBean, GroupChangeLinstener {
    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysGroupMapper sysGroupMapper;
    private final UserManager userManager;

    /**
     * 向群组发送消息<br>
     *
     * @param wsmsg   消息
     * @param sender  发起者
     * @param exclude 排除列表
     */
    public void sendGroup(WsMsg wsmsg, WsUser sender, String... exclude) {
        List<String> list = Arrays.asList(exclude);
        String uid = sender.getUid();
        // 向群内所有人发送消息
        for (String tuid : getGroupUser(wsmsg.getTarget())) {
            // 过滤自己 || 已屏蔽
            if (uid.equals(tuid) || list.contains(tuid)) {
                continue;
            }
            // 发送
            userManager.getUser(tuid).send(wsmsg);
        }
    }

    /**
     * 获取群组对象
     */
    public SysGroup getSysGroup(String groupId) {
        return this.keySet().stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new SocketException(Callback.USER_NOT_FOUND.get(), MessageType.DANGER));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param groupId 群组id
     * @return 成员
     */
    public List<String> getGroupUser(String groupId) {
        return this.get(getSysGroup(groupId));
    }

    @Override
    public void onGroupChange(GroupChangeEvent event) {
        SysGroupUser groupUser = event.getGroupUser();
        SysGroup group = event.getGroup();
        switch (event.getOperation()) {
            case CREATE:
                this.put(group, new ArrayList<>());
                break;
            case DISSOLUTION:
                this.remove(getSysGroup(group.getGroupId()));
                break;
            case JOIN:
                getGroupUser(groupUser.getGroupId()).add(groupUser.getUid());
                break;
            case DELETE:
                getGroupUser(groupUser.getGroupId()).remove(groupUser.getUid());
                break;
            default:
                // ignore
        }
    }

    @Override
    public void afterPropertiesSet() {
        // 缓存群组
        List<SysGroup> sysGroups = sysGroupMapper.selectList(Wrappers.emptyWrapper());
        List<SysGroupUser> groupthis = sysGroupUserMapper.selectList(Wrappers.emptyWrapper());
        for (SysGroup group : sysGroups) {
            List<String> collect = groupthis.stream()
                    .filter(e -> e.getGroupId().equals(group.getGroupId()))
                    .map(SysGroupUser::getUid)
                    .collect(Collectors.toList());
            this.put(group, collect);
        }
    }
}
