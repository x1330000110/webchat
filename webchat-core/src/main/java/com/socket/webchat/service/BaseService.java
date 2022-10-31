package com.socket.webchat.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

public interface BaseService<T> extends IService<T> {
    /**
     * 获取扫描的第一条结果，与{@link #getOne(Wrapper)}不同，
     * 一旦结果匹配立即返回，这通常拥有更好的性能，
     * 如果您想获取匹配的第一条结果而不在乎其他匹配条目导致的问题请使用此方法
     *
     * @param wrapper 条件
     * @return 结果
     */
    default T getFirst(AbstractWrapper<T, ?, ?> wrapper) {
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}
