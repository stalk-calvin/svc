package com.calvin.auditservice.service;

import com.calvin.auditservice.exception.NotFoundException;
import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.model.UserAcl;
import com.calvin.auditservice.model.UserAclAllowedEntities;
import com.calvin.auditservice.repository.AuditLogRepository;
import com.calvin.auditservice.repository.UserAclRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserAclRepository userAclRepository;

    @Mock
    private Gson gson;

    @InjectMocks
    private AuditLogService auditLogService;
    @Test
    void shouldCreateEventLog() {
        String eventId = "event123";
        AuditLog log = AuditLog.builder().id(1L).eventId(eventId).build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(log);

        AuditLog savedLog = auditLogService.saveAuditLog(eventId, "create", "service1",
                "user1", "user1", "entity1", "{}", "{}", "create");

        assertNotNull(savedLog);
        assertEquals(eventId, savedLog.getEventId());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void shouldThrowExceptionWhenUserAclNotFound() {
        String userId = "user1";
        when(userAclRepository.findByUserId(userId)).thenReturn(null);

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                auditLogService.getAuditLogsByUser(userId)
        );

        assertEquals("UserAcl not found", exception.getMessage());
    }

    @Test
    void shouldReturnAllLogsWhenUserIsAdmin() {
        String userId = "admin-user-id";
        UserAcl adminAcl = new UserAcl();
        adminAcl.setAdmin(true);

        List<AuditLog> allLogs = List.of(
            AuditLog.builder().id(1L).eventId("event1").build(),
            AuditLog.builder().id(1L).eventId("event2").build()
        );

        when(userAclRepository.findByUserId(userId)).thenReturn(adminAcl);
        when(auditLogRepository.findAll()).thenReturn(allLogs);

        List<AuditLog> result = auditLogService.getAuditLogsByUser(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnFilteredLogsWhenUserHasLimitedAccess() {
        String userId = "non-admin-user-id";
        UserAcl userAcl = new UserAcl();
        userAcl.setAdmin(false);

        UserAclAllowedEntities entity1 = UserAclAllowedEntities.builder().allowedEntity("entity1").userAcl(userAcl).build();
        UserAclAllowedEntities entity2 = UserAclAllowedEntities.builder().allowedEntity("entity2").userAcl(userAcl).build();
        userAcl.setAllowedEntities(List.of(entity1, entity2));

        List<AuditLog> filteredLogs = List.of(
            AuditLog.builder().id(1L).eventId("event1").build(),
            AuditLog.builder().id(1L).eventId("event2").build()
        );

        when(userAclRepository.findByUserId(userId)).thenReturn(userAcl);
        when(auditLogRepository.findByEntityTypeIn(List.of("entity1", "entity2"))).thenReturn(filteredLogs);

        List<AuditLog> result = auditLogService.getAuditLogsByUser(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository, times(1)).findByEntityTypeIn(List.of("entity1", "entity2"));
    }

    @Test
    void shouldProcessAuditEventAndSaveLog() {
        String message = """
            {
                "eventId": "event123",
                "eventType": "CREATE",
                "serviceName": "service1",
                "userId": "user1",
                "entityId": "entity1",
                "entityType": "entity1",
                "oldValue": "{}",
                "newValue": "{}",
                "action": "create"
            }
            """;

        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);

        AuditLog auditLog = AuditLog.builder().id(1L).eventId("event123").build();

        // Mock parseAuditLog behavior
        when(gson.fromJson(message, JsonObject.class)).thenReturn(jsonObject);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog);

        // Process the audit event
        auditLogService.processAuditEvent(message);

        // Then verify the log was saved
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}