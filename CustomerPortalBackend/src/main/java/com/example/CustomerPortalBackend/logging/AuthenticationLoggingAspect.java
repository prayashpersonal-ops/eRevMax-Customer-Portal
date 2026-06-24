package com.example.CustomerPortalBackend.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuthenticationLoggingAspect {

    @AfterReturning("execution(* *.login*(..))")
    public void loginSuccess(JoinPoint joinPoint) {
        log.info("Authentication Success: {}", joinPoint.getSignature().getName());
    }

    @AfterThrowing(pointcut = "execution(* *.login*(..))", throwing = "ex")
    public void loginFailure(JoinPoint joinPoint,Exception ex) {
        log.warn("Authentication Failed: {}", joinPoint.getSignature().getName());
    }
}