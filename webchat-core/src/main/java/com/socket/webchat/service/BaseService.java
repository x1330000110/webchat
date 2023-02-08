package com.socket.webchat.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

public interface BaseService<T> extends IService<T> {
    /**
     * 检查指定条件的数据在数据库中是否存在，此方法拥有更好的性能 <br>
     * 引用了{@link #getFirst(AbstractWrapper)}，除特殊情况应始终使用此方法检查数据存在  <br>
     * 而不是<code>{@link #getOne(Wrapper)}!=null<code/> 或 <code>{@link #count(Wrapper)} == 0<code/>
     *
     * @param wrapper 条件
     * @return 是否存在
     */
    default boolean exist(AbstractWrapper<T, ?, ?> wrapper) {
        return getFirst(wrapper) != null;
    }

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
