package com.calvin.auditservice.service;

import java.time.Instant;
import java.util.List;

import com.calvin.auditservice.exception.BadRequestException;
import com.calvin.auditservice.exception.NotFoundException;
import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.model.UserAcl;
import com.calvin.auditservice.model.UserAclAllowedEntities;
import com.calvin.auditservice.repository.AuditLogRepository;
import com.calvin.auditservice.repository.UserAclRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    @Autowired
    private final Gson gson;
    @Autowired
    private UserAclRepository userAclRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Method to process the audit events from Kafka
    @KafkaListener(topics = "audit-events", groupId = "audit-service")
    public void processAuditEvent(String message) {
        // Parse the message (JSON)
        AuditLog auditLog = parseAuditLog(message);
        auditLogRepository.save(auditLog);
        log.info("Kafka Audit Event Created: {}", auditLog);
    }

    // Retrieve audit logs based on userId
    public List<AuditLog> getAuditLogsByUser(String userId) {
        UserAcl userAcl = userAclRepository.findByUserId(userId);
        if (userAcl == null) {
            throw new NotFoundException("UserAcl not found");
        }

        if (userAcl.isAdmin()) {
            return auditLogRepository.findAll();
        }

        // Otherwise, get the list of entities the user has access to
        // Return the audit logs filtered by entities the user can access
        List<String> accessibleEntities = userAcl.getAllowedEntities().stream()
                .map(UserAclAllowedEntities::getAllowedEntity)  // Extracting allowedEntity field
                .toList();
        return auditLogRepository.findByEntityIn(accessibleEntities);
    }

    public AuditLog createAuditLog(String eventId, String eventType, String serviceName, String userId,
                                   String entity, String oldValue, String newValue, String action) {
        return auditLogRepository.save(AuditLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .serviceName(serviceName)
                .timestamp(Instant.now())
                .userId(userId)
                .entity(entity)
                .oldValue(oldValue)
                .newValue(newValue)
                .action(action)
                .build());
    }

    private AuditLog parseAuditLog(String message) {
        JsonObject jsonObject = gson.fromJson(message, JsonObject.class);

        String eventId = jsonObject.get("eventId").getAsString();
        String eventType = jsonObject.get("eventType").getAsString();
        String serviceName = jsonObject.get("serviceName").getAsString();
        String userId = jsonObject.get("userId").getAsString();
        String entity = jsonObject.get("entity").getAsString();

        // Parse oldValue and newValue as JsonElement (can be JsonObject, JsonArray, JsonPrimitive, or JsonNull)
        JsonElement oldValueElement = jsonObject.get("oldValue");
        JsonElement newValueElement = jsonObject.get("newValue");

        // Convert JsonElement to String (or keep as JsonElement if needed)
        String oldValue = oldValueElement != null && !oldValueElement.isJsonNull() ? oldValueElement.toString() : null;
        String newValue = newValueElement != null && !newValueElement.isJsonNull() ? newValueElement.toString() : null;

        String action = jsonObject.get("action").getAsString();

        return createAuditLog(eventId, eventType, serviceName, userId, entity, oldValue, newValue, action);
    }
}
