package com.socket.core.custom;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Slf4j
@Configuration
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject object) {
        this.strictInsertFill(object, "createTime", Date.class, new Date());
    }

    @Override
    public void updateFill(MetaObject object) {
        this.strictUpdateFill(object, "updateTime", Date.class, new Date());
    }
}