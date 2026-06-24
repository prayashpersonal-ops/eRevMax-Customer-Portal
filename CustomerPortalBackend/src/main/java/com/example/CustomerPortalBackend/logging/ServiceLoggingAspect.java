package com.example.CustomerPortalBackend.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.example.CustomerPortalBackend.service..*(..))")
    public Object logService(ProceedingJoinPoint joinPoint)throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        log.info("Business Operation Started: {}", method);
        try {
            Object result = joinPoint.proceed();
            log.info("Business Operation Completed: {}", method);
            return result;
        } catch (Exception ex) {
            log.error("Business Operation Failed: {}", method);
            throw ex;
        }
    }
}