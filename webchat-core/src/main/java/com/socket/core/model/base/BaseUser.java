package com.socket.core.model.base;

import com.socket.core.util.Wss;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户与群组公共实现
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseUser extends BaseModel implements Serializable {
    /**
     * 唯一id
     */
    @EqualsAndHashCode.Include
    private String guid;
    /**
     * 昵称/群组名称
     */
    private String name;
    /**
     * 头像/群组头像
     */
    private String headimgurl;

    /**
     * 检查此对象是否为群组
     */
    public boolean isGroup() {
        return Wss.isGroup(guid);
    }
}
