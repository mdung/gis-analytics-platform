package com.example.gis.aspect;

import com.example.gis.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    private final AuditService auditService;

    @Around("@annotation(com.example.gis.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        Object result = null;
        Exception exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Determine action and entity type from method name
            String action = determineAction(methodName);
            String entityType = determineEntityType(joinPoint.getTarget().getClass().getSimpleName());
            UUID entityId = extractEntityId(args);
            
            Map<String, Object> details = new HashMap<>();
            details.put("method", methodName);
            details.put("executionTimeMs", executionTime);
            if (exception != null) {
                details.put("error", exception.getMessage());
            }
            
            auditService.logEvent(action, entityType, entityId, request, details);
        }
    }
    
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    private String determineAction(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return "CREATE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else if (methodName.startsWith("find") || methodName.startsWith("get") || methodName.startsWith("list")) {
            return "VIEW";
        } else if (methodName.contains("Query")) {
            return "QUERY";
        }
        return "UNKNOWN";
    }
    
    private String determineEntityType(String className) {
        if (className.contains("Feature")) {
            return "FEATURE";
        } else if (className.contains("Layer")) {
            return "LAYER";
        } else if (className.contains("Geofence")) {
            return "GEOFENCE";
        } else if (className.contains("Device")) {
            return "DEVICE";
        } else if (className.contains("User")) {
            return "USER";
        }
        return "UNKNOWN";
    }
    
    private UUID extractEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }
        return null;
    }
}

