package com.socket.webchat.model.condition;

import lombok.Data;

@Data
public class EmailCondition {
    /**
     * 邮箱
     */
    private String user;
    /**
     * 原邮箱验证码
     */
    private String selfcode;
    /**
     * 新邮箱验证码
     */
    private String newcode;
}
