package com.fsa.aicenter.domain.admin.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员用户聚合根
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码(BCrypt加密)
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态(1:启用 0:禁用)
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 删除标记(0:未删除 1:已删除)
     */
    private Integer isDeleted;

    /**
     * 用户状态枚举
     */
    public enum Status {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 判断用户是否启用
     */
    public boolean isEnabled() {
        return this.status != null && Status.ENABLED.getCode() == this.status;
    }

    /**
     * 设置用户状态
     *
     * @param status 状态值(0:禁用 1:启用)
     * @throws IllegalArgumentException 如果状态值非法
     */
    public void setStatus(Integer status) {
        if (status != null && status != Status.DISABLED.getCode() && status != Status.ENABLED.getCode()) {
            throw new IllegalArgumentException("Invalid status value: " + status + ", must be 0 or 1");
        }
        this.status = status;
    }

    /**
     * 更新最后登录信息
     */
    public void updateLoginInfo(String ip) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
    }
}
