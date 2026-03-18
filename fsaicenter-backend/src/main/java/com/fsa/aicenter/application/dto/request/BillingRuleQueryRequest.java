package com.fsa.aicenter.application.dto.request;

import lombok.Data;

/**
 * 计费规则查询请求
 */
@Data
public class BillingRuleQueryRequest {

    /** 页码，默认1 */
    private Integer pageNum = 1;

    /** 每页数量，默认10 */
    private Integer pageSize = 10;

    /** 模型ID */
    private Long modelId;

    /** 计费类型 */
    private String billingType;

    /** 状态 */
    private Integer status;
}
