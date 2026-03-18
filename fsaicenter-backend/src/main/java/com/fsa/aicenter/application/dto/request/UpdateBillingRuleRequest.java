package com.fsa.aicenter.application.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新计费规则请求
 */
@Data
public class UpdateBillingRuleRequest {

    /** 单位价格（非TOKEN类型使用） */
    private BigDecimal unitPrice;

    /** 输入单价（TOKEN类型使用） */
    private BigDecimal inputUnitPrice;

    /** 输出单价（TOKEN类型使用） */
    private BigDecimal outputUnitPrice;

    /** 计费单位数量 */
    private Integer unitAmount;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 描述 */
    private String description;
}
