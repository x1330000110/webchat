package com.socket.core.model.condition;

import lombok.Data;

@Data
public class EmailCondition {
    /**
     * 邮箱
     */
    private String email;
    /**
     * 原邮箱验证码
     */
    private String ocode;
    /**
     * 新邮箱验证码
     */
    private String ncode;
}
