package com.socket.webchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.socket.webchat.model.SysUserLog;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserLogMapper extends BaseMapper<SysUserLog> {
    /**
     * 获取每个用户的最新登录日志
     */
    @Select("SELECT guid,province,create_time FROM sys_user_log WHERE id IN ( SELECT MAX(id) FROM sys_user_log where province is not null GROUP BY guid ) ORDER BY guid")
    List<SysUserLog> selectLatestLogonLogs();
}
