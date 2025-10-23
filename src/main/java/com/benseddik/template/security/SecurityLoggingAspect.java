package com.benseddik.template.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SecurityLoggingAspect {

    @Before("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void logSecurityEvent(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "ANONYMOUS";
        String method = joinPoint.getSignature().getName();
        log.info("Security access: user={}, method={}", username, method);
    }
}
