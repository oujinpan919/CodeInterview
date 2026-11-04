package com.codeinterview.common.handler;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MetaObjectHandler implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {

    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, CREATED_AT, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATED_AT, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATED_AT, LocalDateTime.class, LocalDateTime.now());
    }
}
