package com.fsa.aicenter.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.domain.admin.aggregate.AdminOperationLog;
import com.fsa.aicenter.domain.admin.repository.AdminOperationLogRepository;
import com.fsa.aicenter.infrastructure.util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 操作日志切面
 *
 * @author FSA AI Center
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AdminOperationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知,记录操作日志
     */
    @Around("@annotation(com.fsa.aicenter.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行方法
        Object result;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            // 异步记录日志
            saveLogAsync(joinPoint, startTime, exception);
        }

        return result;
    }

    /**
     * 异步保存操作日志
     */
    @Async("asyncExecutor")
    public void saveLogAsync(ProceedingJoinPoint joinPoint, long startTime, Throwable exception) {
        try {
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLog annotation = method.getAnnotation(OperationLog.class);

            // 获取请求信息
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 构建日志对象
            AdminOperationLog.AdminOperationLogBuilder logBuilder = AdminOperationLog.builder();
            logBuilder.operation(annotation.operation());
            logBuilder.method(signature.getDeclaringTypeName() + "." + signature.getName());

            // 获取用户信息
            if (StpUtil.isLogin()) {
                logBuilder.userId(StpUtil.getLoginIdAsLong());
                // 可以从数据库查询username，这里暂时不设置
            }

            // 获取请求参数(限制长度防止过大)
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String paramsJson = objectMapper.writeValueAsString(args);
                // 参数过长时截断
                if (paramsJson.length() > 2000) {
                    paramsJson = paramsJson.substring(0, 2000) + "...";
                }
                logBuilder.params(paramsJson);
            }

            // 获取IP和User-Agent
            if (request != null) {
                logBuilder.ip(IpUtil.getClientIp(request));
                logBuilder.userAgent(request.getHeader("User-Agent"));
            }

            // 执行时长
            logBuilder.executeTime((int) (System.currentTimeMillis() - startTime));

            // 状态和错误信息
            if (exception == null) {
                logBuilder.status(AdminOperationLog.Status.SUCCESS.getCode());
            } else {
                logBuilder.status(AdminOperationLog.Status.FAILED.getCode());
                // 错误信息也限制长度
                String errorMsg = exception.getMessage();
                if (errorMsg != null && errorMsg.length() > 500) {
                    errorMsg = errorMsg.substring(0, 500) + "...";
                }
                logBuilder.errorMsg(errorMsg);
            }

            // 保存日志
            AdminOperationLog opLog = logBuilder.build();
            logRepository.save(opLog);

            log.debug("操作日志记录成功: operation={}, userId={}, executeTime={}ms",
                     opLog.getOperation(), opLog.getUserId(), opLog.getExecuteTime());

        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
}
