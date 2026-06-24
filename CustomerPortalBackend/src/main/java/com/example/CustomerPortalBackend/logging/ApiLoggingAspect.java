package com.example.CustomerPortalBackend.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logApiExecution(ProceedingJoinPoint joinPoint)throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().getName();
        log.info("API Started: {}", method);
        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.info("API Success | Method={} | Time={} ms", joinPoint.getSignature().getName(), time);

            if (time > 2000) {
                log.warn("Slow API Detected | Method={} | Time={} ms",
                        joinPoint.getSignature().getName(),time);
            }
            return result;
        } catch (Exception ex) {
            log.error("API Failed: {}",method);
            throw ex;
        }
    }
}