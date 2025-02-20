package com.calvin.auditservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_entity_type", columnList = "entity_type")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private String eventType;
    private String serviceName;
    private Instant timestamp;
    private String userId;
    private String entityId;
    private String entityType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    private String action;
}