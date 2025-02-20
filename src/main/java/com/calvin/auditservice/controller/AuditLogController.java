package com.calvin.auditservice.controller;

import com.calvin.auditservice.exception.ApiResponse;
import com.calvin.auditservice.exception.BadRequestException;
import com.calvin.auditservice.exception.UnauthorizedException;
import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class AuditLogController {
    private final AuditLogService auditService;

    @Autowired
    public AuditLogController(AuditLogService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs/{userId}")
    public List<AuditLog> getAuditLogsByUserId(@PathVariable("userId") String userId) {
        if (!isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required to retrieve the audit log.");
        }

        return auditService.getAuditLogsByUser(userId);
    }

    @PostMapping("/logs")
    public ResponseEntity<ApiResponse> createAuditLog(@RequestBody AuditLog auditLog) {
        // Validate input
        if (auditLog.getEventId() == null || auditLog.getEventType() == null ||
            auditLog.getEntityId() == null || auditLog.getEntityType() == null) {
            throw new BadRequestException("Invalid input data.");
        }

        // Simulate authentication check
        if (!isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required to create an audit log.");
        }

        auditService.saveAuditLog(
            auditLog.getEventId(),
            auditLog.getEventType(),
            auditLog.getServiceName(),
            auditLog.getUserId(),
            auditLog.getEntityId(),
            auditLog.getEntityType(),
            auditLog.getOldValue(),
            auditLog.getNewValue(),
            auditLog.getAction()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Audit log created successfully"));
    }

    private boolean isAuthenticated() {
        // TODO: Replace with real authentication logic
        return true;
    }
}