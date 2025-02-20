package com.calvin.auditservice.controller;

import com.calvin.auditservice.exception.UnauthorizedException;
import com.calvin.auditservice.model.AuditLog;
import com.calvin.auditservice.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, controlledShutdown = true)
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
class AuditLogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogController auditLogController;

    @Test
    void shouldGetAuditLogsByUserId() throws Exception {
        List<AuditLog> logs = List.of(AuditLog.builder().id(1L).eventId("event123").build());
        when(auditLogService.getAuditLogsByUser("user1")).thenReturn(logs);

        mockMvc.perform(get("/v1/logs/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].eventId").value("event123"));
    }

    @Test
    void shouldReturnUnauthorizedWhenFetchingLogsWithoutAuth() throws Exception {
        doThrow(new UnauthorizedException("Authentication is required")).when(auditLogService).getAuditLogsByUser(anyString());

        mockMvc.perform(get("/v1/logs/user1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void shouldCreateAuditLogSuccessfully() throws Exception {
        AuditLog auditLog = AuditLog.builder().id(1L).eventId("event123").build();
        when(auditLogService.saveAuditLog(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(auditLog);

        mockMvc.perform(post("/v1/logs")
                        .contentType("application/json")
                        .content("""
                            {
                                "eventId": "event123",
                                "eventType": "CREATE",
                                "serviceName": "service1",
                                "userId": "user1",
                                "entity": "entity1",
                                "oldValue": "{}",
                                "newValue": "{}",
                                "action": "create"
                            }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Audit log created successfully"));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingInvalidAuditLog() throws Exception {
        mockMvc.perform(post("/v1/logs")
                        .contentType("application/json")
                        .content("""
                            {
                                "eventType": "CREATE"
                            }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid input data."));
    }
}
