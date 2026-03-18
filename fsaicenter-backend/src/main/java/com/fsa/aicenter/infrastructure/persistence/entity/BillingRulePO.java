package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计费规则持久化对象
 */
@Data
@TableName("billing_rule")
public class BillingRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_id")
    private Long modelId;

    /**
     * 计费类型: TOKEN, IMAGE, AUDIO_DURATION
     */
    @TableField("billing_type")
    private String billingType;

    /**
     * 单位价格（元）- 6位小数
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 输入单价（元），仅 TOKEN 类型使用
     */
    @TableField("input_unit_price")
    private BigDecimal inputUnitPrice;

    /**
     * 输出单价（元），仅 TOKEN 类型使用
     */
    @TableField("output_unit_price")
    private BigDecimal outputUnitPrice;

    /**
     * 计费单位数量，如1000个Token
     */
    @TableField("unit_amount")
    private Integer unitAmount;

    @TableField("currency")
    private String currency;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("description")
    private String description;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
