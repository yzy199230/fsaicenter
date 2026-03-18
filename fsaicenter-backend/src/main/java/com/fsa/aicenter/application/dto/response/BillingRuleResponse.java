package com.fsa.aicenter.application.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计费规则响应
 */
@Data
public class BillingRuleResponse {

    private Long id;

    private Long modelId;

    /** 模型名称 */
    private String modelName;

    /** 模型类型 */
    private String modelType;

    /** 计费类型 */
    private String billingType;

    /** 单位价格 */
    private BigDecimal unitPrice;

    /** 输入单价 */
    private BigDecimal inputUnitPrice;

    /** 输出单价 */
    private BigDecimal outputUnitPrice;

    /** 计费单位数量 */
    private Integer unitAmount;

    /** 货币 */
    private String currency;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 描述 */
    private String description;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
