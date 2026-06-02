package com.ai.exception;

import com.ai.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，根据 code 动态设置 HTTP 状态码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        // 使用业务异常里指定的状态码，消息原样返回
        return ResponseEntity.status(e.getCode())
                .body(Result.error(e.getMessage()));
    }

    /**
     * 处理运行时异常（未捕获的系统级错误）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result> handleRuntimeException(RuntimeException e) {
        log.error("系统运行时异常：", e);
        return ResponseEntity.status(500)
                .body(Result.error("系统繁忙，请稍后重试或联系管理员"));
    }

    /**
     * 兜底处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        log.error("未预料的异常：", e);
        return ResponseEntity.status(500)
                .body(Result.error("出错啦，请联系管理员~"));
    }
}