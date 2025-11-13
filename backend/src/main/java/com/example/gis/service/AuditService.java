package com.example.gis.service;

import com.example.gis.entity.AuditLog;
import com.example.gis.entity.User;
import com.example.gis.repository.AuditLogRepository;
import com.example.gis.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an audit event asynchronously
     */
    @Async
    public void logEvent(String action, String entityType, UUID entityId, 
                        HttpServletRequest request, Map<String, Object> details) {
        try {
            AuditLog auditLog = buildAuditLog(action, entityType, entityId, request, details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log CRUD operation
     */
    @Async
    public void logCrudOperation(String action, String entityType, UUID entityId, 
                                HttpServletRequest request, Map<String, Object> details) {
        logEvent(action, entityType, entityId, request, details);
    }

    /**
     * Log query operation
     */
    @Async
    public void logQuery(String queryType, String entityType, HttpServletRequest request, 
                        Map<String, Object> queryParams, Long executionTimeMs) {
        Map<String, Object> details = Map.of(
                "queryType", queryType,
                "executionTimeMs", executionTimeMs != null ? executionTimeMs : 0L
        );
        logEvent("QUERY", entityType, null, request, details);
    }

    /**
     * Log bbox query
     */
    @Async
    public void logBboxQuery(String entityType, UUID layerId, double minLng, double minLat, 
                           double maxLng, double maxLat, HttpServletRequest request, Long executionTimeMs) {
        Map<String, Object> details = Map.of(
                "layerId", layerId.toString(),
                "bbox", Map.of("minLng", minLng, "minLat", minLat, "maxLng", maxLng, "maxLat", maxLat),
                "executionTimeMs", executionTimeMs != null ? executionTimeMs : 0L
        );
        logEvent("QUERY", entityType, layerId, request, details);
    }

    /**
     * Log user action
     */
    @Async
    public void logUserAction(String action, HttpServletRequest request, Map<String, Object> details) {
        logEvent(action, "USER", null, request, details);
    }

    /**
     * Build audit log from request and details
     */
    private AuditLog buildAuditLog(String action, String entityType, UUID entityId,
                                  HttpServletRequest request, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        String username = null;
        UUID userId = null;

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            username = auth.getName();
            user = userRepository.findByUsernameAndDeletedAtIsNull(username).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        String queryParams = null;
        if (request.getQueryString() != null) {
            queryParams = request.getQueryString();
        }

        String detailsJson = null;
        if (details != null && !details.isEmpty()) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (Exception e) {
                log.warn("Failed to serialize audit details: {}", e.getMessage());
            }
        }

        return AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .queryParams(queryParams)
                .details(detailsJson)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Get audit logs for user
     */
    public Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get audit logs by action
     */
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }

    /**
     * Get audit logs for entity
     */
    public Page<AuditLog> getAuditLogsByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }

    /**
     * Get audit logs by date range
     */
    public Page<AuditLog> getAuditLogsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

