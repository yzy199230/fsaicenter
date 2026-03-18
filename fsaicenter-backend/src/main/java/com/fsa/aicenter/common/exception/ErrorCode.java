package com.fsa.aicenter.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author FSA AI Center
 */
@Getter
public enum ErrorCode {

    // ============================================
    // 通用错误码 (1000-1999)
    // ============================================
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统错误"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // ============================================
    // API Key相关错误码 (2000-2099)
    // ============================================
    API_KEY_INVALID(2001, "API Key无效"),
    API_KEY_EXPIRED(2002, "API Key已过期"),
    API_KEY_QUOTA_EXCEEDED(2003, "API Key配额已用尽"),
    API_KEY_RATE_LIMIT_EXCEEDED(2004, "API Key请求频率超限"),
    API_KEY_IP_NOT_ALLOWED(2005, "IP地址不在白名单中"),
    API_KEY_MODEL_NOT_ALLOWED(2006, "无权访问该模型"),
    API_KEY_NOT_FOUND(2007, "API Key不存在"),

    // ============================================
    // 模型相关错误码 (2100-2199)
    // ============================================
    MODEL_NOT_FOUND(2101, "模型不存在"),
    MODEL_DISABLED(2102, "模型已禁用"),
    MODEL_NOT_SUPPORT_STREAM(2103, "模型不支持流式响应"),
    MODEL_FALLBACK_FAILED(2104, "模型降级失败"),
    MODEL_CONFIG_ERROR(2105, "模型配置错误"),
    MODEL_CODE_EXISTS(2106, "模型编码已存在"),
    MODEL_KEY_NOT_FOUND(2107, "模型API Key不存在"),
    MODEL_NO_AVAILABLE_KEY(2108, "模型没有可用的API Key"),
    MODEL_TYPE_NOT_SUPPORTED(2109, "模型类型不支持"),
    MODEL_ALREADY_EXISTS(2110, "模型已存在"),

    // ============================================
    // 模板相关错误码 (2150-2199)
    // ============================================
    TEMPLATE_NOT_FOUND(2151, "模板不存在"),
    TEMPLATE_NOT_EDITABLE(2152, "该模板不可编辑"),
    TEMPLATE_NOT_DELETABLE(2153, "该模板不可删除"),

    // ============================================
    // 提供商相关错误码 (2200-2299)
    // ============================================
    PROVIDER_NOT_FOUND(2201, "AI提供商不存在"),
    PROVIDER_DISABLED(2202, "AI提供商已禁用"),
    PROVIDER_API_KEY_MISSING(2203, "提供商API Key未配置"),
    PROVIDER_REQUEST_FAILED(2204, "调用提供商API失败"),
    PROVIDER_TIMEOUT(2205, "调用提供商API超时"),
    PROVIDER_CODE_EXISTS(2206, "提供商编码已存在"),
    ADAPTER_NOT_FOUND(2207, "适配器不存在"),
    DISCOVERY_NOT_SUPPORTED(2208, "该提供商不支持自动发现模型"),

    // ============================================
    // 计费相关错误码 (2300-2399)
    // ============================================
    BILLING_RULE_NOT_FOUND(2301, "计费规则不存在"),
    BILLING_FAILED(2302, "计费失败"),

    // ============================================
    // 数据相关错误码 (2900-2999)
    // ============================================
    DATA_NOT_FOUND(2901, "数据不存在"),

    // ============================================
    // 数据库相关错误码 (3000-3099)
    // ============================================
    DB_ERROR(3001, "数据库操作失败"),
    DB_DUPLICATE_KEY(3002, "数据已存在"),

    // ============================================
    // 缓存相关错误码 (3100-3199)
    // ============================================
    CACHE_ERROR(3101, "缓存操作失败"),

    // ============================================
    // 认证相关错误码 (4000-4099)
    // ============================================
    USER_NOT_FOUND(4001, "用户不存在"),
    PASSWORD_ERROR(4002, "密码错误"),
    USER_DISABLED(4003, "用户已被禁用"),
    USERNAME_EXISTS(4004, "用户名已存在"),
    OPERATION_FORBIDDEN(4005, "操作被禁止"),

    // ============================================
    // 角色相关错误码 (4050-4099)
    // ============================================
    ROLE_NOT_FOUND(4051, "角色不存在"),
    ROLE_CODE_EXISTS(4052, "角色编码已存在"),

    // ============================================
    // 文件相关错误码 (4100-4199)
    // ============================================
    FILE_UPLOAD_FAILED(4101, "文件上传失败"),
    FILE_SIZE_EXCEEDED(4102, "文件大小超限"),
    FILE_TYPE_NOT_ALLOWED(4103, "文件类型不支持");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
