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
 * 计费记录持久化对象
 */
@Data
@TableName("billing_record")
public class BillingRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("model_id")
    private Long modelId;

    /**
     * 计费类型: TOKEN, IMAGE, AUDIO_DURATION
     */
    @TableField("billing_type")
    private String billingType;

    /**
     * 使用量：Token数/图片数/音频秒数
     */
    @TableField("usage_amount")
    private Long usageAmount;

    /**
     * 单位价格（元）- 6位小数
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 总费用（元）- 4位小数
     */
    @TableField("total_cost")
    private BigDecimal totalCost;

    @TableField("currency")
    private String currency;

    @TableField("billing_time")
    private LocalDateTime billingTime;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
