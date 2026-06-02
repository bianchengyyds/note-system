package com.ai.aspect;

import com.ai.annotation.OpLog;
import com.ai.dto.OperationLog;
import com.ai.service.OperationLogService;
import com.ai.utils.CurrentHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OpLogAspect {

    private final OperationLogService operationLogService;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        // 执行目标方法
        Object result = joinPoint.proceed();

        try {
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
            String ip = request != null ? request.getRemoteAddr() : "unknown";
            String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";

            // 获取当前用户ID（从JWT解析）
            Long userId = CurrentHolder.getCurrentId();

            // ✅ 修改：将 result 传入解析方法，以便支持 #result.xxx 表达式
            String targetIdStr = parseTargetId(opLog.targetId(), joinPoint, result);

            // 构建日志实体
            OperationLog logEntity = new OperationLog();
            logEntity.setUserId(userId);
            logEntity.setOperation(opLog.operation());
            logEntity.setTargetType(opLog.targetType());
            logEntity.setTargetId(targetIdStr != null ? Long.valueOf(targetIdStr) : null);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent);
            logEntity.setCreatedAt(LocalDateTime.now());

            // 保存日志
            operationLogService.save(logEntity);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
        return result;
    }

    /**
     * 解析 SpEL 表达式获取目标 ID
     * @param spEl 表达式字符串
     * @param joinPoint 切点
     * @param result 方法返回值
     */
    private String parseTargetId(String spEl, ProceedingJoinPoint joinPoint, Object result) {
        if (spEl == null || spEl.isEmpty()) return null;
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            EvaluationContext context = new StandardEvaluationContext();

            // 1. 放入方法参数
            String[] paramNames = discoverer.getParameterNames(signature.getMethod());
            Object[] args = joinPoint.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            // ✅ 2. 关键：放入返回值，变量名设为 "result"
            // 这样注解中写 targetId = "#result.data.id" 时才能找到 result 对象
            context.setVariable("result", result);

            Expression exp = parser.parseExpression(spEl);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            // ✅ 3. 容错：解析失败只打印警告，不抛出异常阻断业务
            log.warn("解析操作日志 TargetId 失败 [SpEL: {}]: {}", spEl, e.getMessage());
            return null;
        }
    }
}