package com.calvin.auditservice.service;

import java.time.Instant;
import java.util.List;

import com.calvin.auditservice.exception.NotFoundException;
import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.model.UserAcl;
import com.calvin.auditservice.model.UserAclAllowedEntities;
import com.calvin.auditservice.repository.AuditLogRepository;
import com.calvin.auditservice.repository.UserAclRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class AuditLogService {

    @Autowired
    private Gson gson;
    @Autowired
    private UserAclRepository userAclRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Method to process the audit events from Kafka
    @KafkaListener(topics = "audit-events", groupId = "audit-service")
    public void processAuditEvent(String message) {
        // Parse the message (JSON)
        AuditLog auditLog = parseAndCreateAuditLog(message);
        log.info("Kafka Audit Event Created: {}", auditLog);
    }

    // Retrieve audit logs based on userId
    public List<AuditLog> getAuditLogsByUser(String userId) {
        UserAcl userAcl = userAclRepository.findByUserId(userId);
        if (userAcl == null) {
            throw new NotFoundException("UserAcl not found");
        }

        log.info("Is this user ( {} ) admin?: {}", userAcl.getUserId(), userAcl.isAdmin());
        if (userAcl.isAdmin()) {
            return auditLogRepository.findAll();
        }

        // Otherwise, get the list of entities the user has access to
        // Return the audit logs filtered by entities the user can access
        List<String> accessibleEntities = userAcl.getAllowedEntities().stream()
                .map(UserAclAllowedEntities::getAllowedEntity)
                .toList();
        log.info("Filter based on these entities: {}", accessibleEntities);
        return auditLogRepository.findByEntityTypeIn(accessibleEntities);
    }

    public AuditLog saveAuditLog(String eventId, String eventType, String serviceName, String userId,
                                 String entityId, String entityType, String oldValue, String newValue, String action) {
        return auditLogRepository.save(AuditLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .serviceName(serviceName)
                .timestamp(Instant.now())
                .userId(userId)
                .entityId(entityId)
                .entityType(entityType)
                .oldValue(oldValue)
                .newValue(newValue)
                .action(action)
                .build());
    }

    private AuditLog parseAndCreateAuditLog(String message) {
        JsonObject jsonObject = gson.fromJson(message, JsonObject.class);

        String eventId = jsonObject.get("eventId").getAsString();
        String eventType = jsonObject.get("eventType").getAsString();
        String serviceName = jsonObject.get("serviceName").getAsString();
        String userId = jsonObject.get("userId").getAsString();
        String entityId = jsonObject.get("entityId").getAsString();
        String entityType = jsonObject.get("entityType").getAsString();

        // Parse oldValue and newValue as JsonElement (can be JsonObject, JsonArray, JsonPrimitive, or JsonNull)
        JsonElement oldValueElement = jsonObject.get("oldValue");
        JsonElement newValueElement = jsonObject.get("newValue");

        // Convert JsonElement to String (or keep as JsonElement if needed)
        String oldValue = oldValueElement != null && !oldValueElement.isJsonNull() ? oldValueElement.toString() : null;
        String newValue = newValueElement != null && !newValueElement.isJsonNull() ? newValueElement.toString() : null;

        String action = jsonObject.get("action").getAsString();

        return saveAuditLog(eventId, eventType, serviceName, userId, entityId, entityType, oldValue, newValue, action);
    }
}
