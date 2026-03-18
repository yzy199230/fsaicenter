package com.fsa.aicenter.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 用于Controller方法上,记录用户操作日志
 * </p>
 *
 * @author FSA AI Center
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     * <p>
     * 示例: "创建模型", "删除API密钥", "修改角色权限"
     * </p>
     */
    String operation();
}
