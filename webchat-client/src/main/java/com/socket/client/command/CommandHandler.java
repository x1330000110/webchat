package com.socket.client.command;

import org.springframework.context.ApplicationEvent;

/**
 * 命令处理接口 <br>
 * 注意：这个接口只处理与聊天室内部数据有关的内容，其他内容应在此事件之前完成
 */
public interface CommandHandler<E extends ApplicationEvent> {
    /**
     * 执行命令
     *
     * @param command 事件
     */
    void invoke(E command);
}
