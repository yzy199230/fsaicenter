package com.fsa.aicenter.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建计费规则请求
 */
@Data
public class CreateBillingRuleRequest {

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotNull(message = "计费类型不能为空")
    private String billingType;

    /** 单位价格（非TOKEN类型使用） */
    private BigDecimal unitPrice;

    /** 输入单价（TOKEN类型使用） */
    private BigDecimal inputUnitPrice;

    /** 输出单价（TOKEN类型使用） */
    private BigDecimal outputUnitPrice;

    /** 计费单位数量，默认1000 */
    private Integer unitAmount = 1000;

    /** 货币，默认CNY */
    private String currency = "CNY";

    @NotNull(message = "生效时间不能为空")
    private LocalDateTime effectiveTime;

    /** 过期时间，可选 */
    private LocalDateTime expireTime;

    /** 描述 */
    private String description;
}
