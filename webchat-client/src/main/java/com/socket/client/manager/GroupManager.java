package com.socket.client.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.socket.client.exception.SocketException;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.custom.listener.GroupChangeLinstener;
import com.socket.webchat.custom.listener.command.GroupEnum;
import com.socket.webchat.custom.listener.event.GroupChangeEvent;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.Command;
import com.socket.webchat.model.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ws群组管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupManager extends ConcurrentHashMap<SysGroup, List<WsUser>> implements InitializingBean, GroupChangeLinstener {
    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysGroupMapper sysGroupMapper;
    private final UserManager userManager;

    /**
     * 向群组发送消息<br>
     *
     * @param wsmsg 消息
     */
    public void sendGroup(WsMsg wsmsg) {
        for (WsUser target : getGroupUsers(wsmsg.getTarget())) {
            target.send(wsmsg);
        }
    }

    /**
     * @see #sendGroup(String, String, Command, Object)
     */
    public <T extends SysUser> void sendGroup(String groupId, String content, Command<?> type) {
        sendGroup(groupId, content, type, null);
    }

    /**
     * 向群组发送系统消息<br>
     *
     * @param groupId 群id
     * @param content 消息内容
     * @param type    消息类型
     * @param data    额外数据
     */
    public void sendGroup(String groupId, String content, Command<?> type, Object data) {
        for (WsUser wsuser : getGroupUsers(groupId)) {
            wsuser.send(content, type, data);
        }
    }

    /**
     * 获取群组对象
     */
    public SysGroup getGroup(String groupId) {
        return this.keySet().stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new SocketException(Callback.USER_NOT_FOUND.format(groupId), MessageType.DANGER));
    }

    /**
     * 获取指定群组ID成员列表
     *
     * @param groupId 群组id
     * @return 成员
     */
    public List<WsUser> getGroupUsers(String groupId) {
        return this.get(getGroup(groupId));
    }

    @Override
    public void onGroupChange(GroupChangeEvent event) {
        SysGroupUser guser = event.getUser();
        SysGroup group = event.getGroup();
        switch (event.getOperation()) {
            case CREATE:
                this.put(group, new ArrayList<>());
                break;
            case DISSOLVE:
                SysGroup find = getGroup(group.getGroupId());
                String tips = Callback.GROUP_DISSOLVE.format(find.getName());
                sendGroup(group.getGroupId(), tips, GroupEnum.DISSOLVE);
                this.remove(find);
                break;
            case JOIN:
                String uid = guser.getUid(), gid = guser.getGroupId();
                SysGroup sysGroup = getGroup(gid);
                WsUser user = userManager.getUser(uid);
                getGroupUsers(gid).add(user);
                // 发送通知
                sendGroup(gid, gid, GroupEnum.JOIN, user);
                break;
            case DELETE:
                userManager.getUser(guser.getUid()).send("您已被管理员移除群聊", GroupEnum.DELETE);
            case EXIT:
                getGroupUsers(guser.getGroupId()).remove(userManager.getUser(guser.getUid()));
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
            List<WsUser> collect = groupthis.stream()
                    .filter(e -> e.getGroupId().equals(group.getGroupId()))
                    .map(SysGroupUser::getUid)
                    .map(userManager::getUser)
                    .collect(Collectors.toList());
            this.put(group, collect);
        }
    }
}
