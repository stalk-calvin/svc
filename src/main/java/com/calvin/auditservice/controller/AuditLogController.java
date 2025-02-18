package com.calvin.auditservice.controller;

import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit/v1")
public class AuditLogController {

    @Autowired
    private AuditLogService auditService;

    @GetMapping("/logs/{userId}")
    public List<AuditLog> getAuditLogsByUserId(@PathVariable String userId) {
        return auditService.getAuditLogsByUser(userId);
    }

    @PostMapping("/logs")
    public AuditLog createAuditLog(@RequestBody AuditLog auditLog) {
        return auditService.createAuditLog(
                auditLog.getEventId(),
                auditLog.getEventType(),
                auditLog.getServiceName(),
                auditLog.getUserId(),
                auditLog.getEntity(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getAction()
        );
    }
}