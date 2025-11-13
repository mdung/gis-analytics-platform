package com.example.gis.controller;

import com.example.gis.entity.AuditLog;
import com.example.gis.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Logs", description = "Audit logging APIs (ADMIN only)")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs", description = "Get paginated audit logs (ADMIN only)")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            Pageable pageable) {
        
        if (userId != null) {
            return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
        } else if (action != null) {
            return ResponseEntity.ok(auditService.getAuditLogsByAction(action, pageable));
        } else if (entityType != null && entityId != null) {
            return ResponseEntity.ok(auditService.getAuditLogsByEntity(entityType, entityId, pageable));
        } else if (startDate != null && endDate != null) {
            return ResponseEntity.ok(auditService.getAuditLogsByDateRange(startDate, endDate, pageable));
        }
        
        // Default: return all (would need a findAll method)
        return ResponseEntity.ok(auditService.getAuditLogsByAction(null, pageable));
    }
}

