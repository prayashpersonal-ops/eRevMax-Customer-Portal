package com.example.CustomerPortalBackend.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class DatabaseLoggingAspect {

    /**
     * Log repository save operations
     */
    @Before("execution(* com.example.CustomerPortalBackend.repository.*.save(..))")
    public void logBeforeSave(JoinPoint joinPoint) {
        log.info("[DATABASE] Save operation started. Repository={}, Method={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "execution(* com.example.CustomerPortalBackend.repository.*.save(..))", returning = "result")
    public void logAfterSave(JoinPoint joinPoint, Object result) {
        log.info("[DATABASE] Save operation completed. Repository={}, Method={}, Result={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                result != null ? result.getClass().getSimpleName() : "null");
    }

    /**
     * Log delete operations
     */
    @Before("execution(* com.example.CustomerPortalBackend.repository.*.delete*(..))")
    public void logBeforeDelete(JoinPoint joinPoint) {
        log.info("[DATABASE] Delete operation started. Repository={}, Method={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName());
    }

    @AfterReturning("execution(* com.example.CustomerPortalBackend.repository.*.delete*(..))")
    public void logAfterDelete(JoinPoint joinPoint) {
        log.info("[DATABASE] Delete operation completed. Repository={}, Method={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName());
    }

    /**
     * Log database exceptions
     */
    @AfterThrowing(pointcut = "execution(* com.example.CustomerPortalBackend.repository.*.*(..))",
            throwing = "exception")
    public void logDatabaseException(JoinPoint joinPoint, Exception exception) {
        log.error("[DATABASE ERROR] Repository={}, Method={}, Error={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }
}