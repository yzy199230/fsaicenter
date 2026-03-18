package com.fsa.aicenter.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fsa.aicenter.common.annotation.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限验证切面
 * <p>
 * 拦截标注了{@link RequirePermission}注解的方法,进行权限验证
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    /**
     * 在标注了@RequirePermission的方法执行前进行权限验证
     * <p>
     * 如果用户没有对应权限,会抛出NotPermissionException,由全局异常处理器捕获
     * </p>
     *
     * @param joinPoint 切点
     */
    @Before("@annotation(com.fsa.aicenter.common.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation != null) {
            String permission = annotation.value();
            // 调用sa-token的权限验证
            StpUtil.checkPermission(permission);
            log.debug("权限验证通过: permission={}, userId={}", permission, StpUtil.getLoginId());
        }
    }
}
