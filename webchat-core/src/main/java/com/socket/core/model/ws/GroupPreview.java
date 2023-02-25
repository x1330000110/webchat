package com.socket.core.model.ws;

import cn.hutool.core.bean.BeanUtil;
import com.socket.core.model.po.SysGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览群组
 *
 * @date 2021/8/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GroupPreview extends SysGroup {
    /**
     * 是否为群组
     */
    private boolean isgroup;
    /**
     * 群组成员
     */
    private List<String> guids;

    public GroupPreview(SysGroup source) {
        BeanUtil.copyProperties(source, this);
    }
}
