package com.fsa.aicenter.common.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解
 * <p>
 * 用于Controller方法或类上,验证当前用户是否拥有指定权限
 * </p>
 * <p>
 * 使用示例:
 * <pre>
 * {@code
 * @RequirePermission("model:create")
 * public Result<Void> createModel(CreateModelRequest request) {
 *     // 只有拥有"model:create"权限的用户才能访问
 * }
 * }
 * </pre>
 * </p>
 *
 * @author FSA AI Center
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限码
     * <p>
     * 权限码格式: "资源:操作"
     * </p>
     * <p>
     * 示例:
     * <ul>
     *   <li>model:create - 创建模型权限</li>
     *   <li>model:update - 更新模型权限</li>
     *   <li>model:delete - 删除模型权限</li>
     *   <li>apikey:list - 查看API密钥列表权限</li>
     * </ul>
     * </p>
     *
     * @return 权限码
     */
    String value();
}
