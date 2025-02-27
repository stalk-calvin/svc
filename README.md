# Audit Service

The Audit Service is a Spring Boot application designed to monitor and log changes across various services. It listens to change notifications via Kafka and provides secure APIs for accessing audit logs. Stateless API design, event-driven architectures using Kafka.

## Table of Contents

- [Getting Started](#getting-started)
- [Produce test messages in Kafka](#produce-test-messages-in-kafka)
- [Audit Message Format](#audit-message-format)
- [DB Schema Design](#db-schema-design)
- [API Design](#api-design)
- [Authentication and Authorization](#1-retrieve-audit-logs)
- [Audit Tampering Protection](#audit-tampering-protection)
- [Audit Log Rotation](#audit-log-rotation)
- [Testing Strategy](#testing-strategy)
- [Deployment & Scalability Considerations](#deployment-and-scalability-considerations)

## Getting Started

To build and run the application using Docker Compose:

```bash
docker-compose down -v; docker-compose up --build
```

This command shuts down any running containers, rebuilds the images, and starts the services defined in your docker-compose.yml file.

## Produce messages in Kafka

Once your Docker Compose setup is running, produce test messages to Kafka by running the following script:

```bash
./bin/produce_kafka_messages.sh
```

This script will read messages from the specified JSON file and publish them to a Kafka topic.

## Audit Message Format

Audit messages should adhere to the following JSON structure:

```json
{
  "eventId": "unique-identifier",
  "eventType": "type-of-change",
  "serviceName": "service-name",
  "timestamp": "ISO-8601 timestamp",
  "userId": "user performing the action",
  "entityType": "entity type (e.g., Order, User)",
  "entityId": "entity identifier",
  "oldValue": "previous state (JSON object)",
  "newValue": "new state (JSON object)",
  "action": "create/update/delete"
}
```

### Sample message

```json
{
  "eventId": "abc123",
  "eventType": "update",
  "serviceName": "user-service",
  "timestamp": "2025-02-18T10:00:00Z",
  "userId": "user123",
  "entityId": "user123",
  "entityType": "UserProfile",
  "oldValue": "{\"name\": \"Calvin\"}",
  "newValue": "{\"name\": \"Calvin Lee\"}",
  "action": "update"
}
```

## DB Schema Design

The Audit Service utilizes the following schema design:

### Tables:
- **audit_log**: Stores details of all audit events. If the table grows too large, we can consider implementing partitioning or archiving strategies to maintain performance. For example, partitioning by date (e.g., monthly or yearly) or archiving older entries to a separate storage solution can prevent the main table from becoming unwieldy, similar to an LRU (Least Recently Used) strategy.
- **user_acl**: Stores information about users: `user_id` (username), `is_admin` (boolean). This table manages access control by defining user roles and permissions.
- **user_acl_allowed_entities**: Stores information about a user's limited access to specific entities, defining which entities a user can interact with based on their access level.

### Indexes:
- **audit_log**: `"idx_entity_type" btree (entity_type)` — Optimizes filtering of logs based on the `entity_type` column, particularly for non-admin users.
- **user_acl**: `"idx_user_id" btree (user_id)` — Helps retrieve the `user_acl_id` quickly using the `user_id` field.
- **user_acl_allowed_entities**: `FOREIGN KEY (user_acl_id) REFERENCES user_acl(id)` — Ensures referential integrity by linking `user_acl_allowed_entities` to `user_acl`, enabling access to allowed entities for each user.

## API Design

This project provides APIs for creating and retrieving audit logs.

### Swagger UI

You can explore and interact with the available API endpoints through the Swagger UI. This provides an interactive documentation interface.

**Swagger UI URL**:
```text
http://localhost:8080/audit/swagger-ui.html
```

The service offers RESTful APIs to access audit logs:

### 1. Retrieve Audit Logs
APIs to view audit messages, considering two personas for viewing audit message - admin who can view all audit messages; non-admin who can view audits only for entities he/she has access (admin/non-admin controlled in user_acl table.)
#### Endpoint: `GET /audit/v1/logs/admin-user-id`
#### Parameters: `userId (string, required): The unique identifier of the user.`
#### Responses:
- **200 OK**: Returns a list of audit logs. 
- **403 Forbidden**: User does not have permission to view these logs. 
- **404 Not Found**: No logs found for the given userId.
#### Example cURL Request:
```bash
curl -X GET "http://localhost:8080/audit/v1/logs/admin-user-id" \
     -H "Content-Type: application/json"
```
### 2. Create audit log: 
Submit a new audit log entry following the specified message format.
#### Endpoint: `POST /audit/v1/logs`
#### Request Body: 
The request body must follow this JSON structure:
```json
{
  "eventId": "abc123",
  "eventType": "update",
  "serviceName": "user-service",
  "timestamp": "2025-02-18T10:00:00Z",
  "userId": "user123",
  "entityId": "user123",
  "entityType": "UserProfile",
  "oldValue": "{\"name\": \"Calvin\"}",
  "newValue": "{\"name\": \"Calvin Lee\"}",
  "action": "update"
}
```
#### Responses:
- **201 Created**: Audit log created successfully.
- **400 Bad Request**: Invalid input data.
- **401 Unauthorized**: Authentication is required to create an audit log.
#### Example cURL Request:
```bash
curl -X POST "http://localhost:8080/audit/v1/logs" \
     -H "Content-Type: application/json" \
     -d '{
  "eventId": "abc123",
  "eventType": "update",
  "serviceName": "user-service",
  "timestamp": "2025-02-18T10:00:00Z",
  "userId": "user123",
  "entityId": "user123",
  "entityType": "UserProfile",
  "oldValue": "{\"name\": \"Calvin\"}",
  "newValue": "{\"name\": \"Calvin Lee\"}",
  "action": "update"
}'
```

Note: API security is enforced using Spring Security with JWT or OAuth2 for access control.

## Authentication and Authorization

### Current Setup

Authentication is currently hardcoded for simplicity.

### Proposed Setup - OAuth2

- OAuth2-Based Authentication
  - To improve security, OAuth2 can be integrated with providers like ORY Hydra, Okta, or Google. Users authenticate via an IdP, which issues JWT tokens for validation in the audit service.

- Authorization & Role-Based Access Control (RBAC)
  - Admins can access all audit logs.
  - Non-admins can access allowed (by entity name) audit logs.

## Audit Log Rotation

To manage log retention and ensure efficient storage usage, the audit logging system supports automatic log rotation with configurable settings.

- **Configurable Retention Window**: Retain logs for 30 days before rotation, with a total size cap of 2GB.
- **Size-Based Rotation**: Individual log files are capped at 100MB, preventing excessively large files.
- **Time-Based Rotation**: Logs are rotated daily (audit-service-YYYY-MM-DD-i.log format).
- **Storage Path**: Logs are stored in /var/log/audit/ for centralized access and management.

## Audit Tampering Protection

Ensuring the integrity and immutability of audit logs is crucial to prevent unauthorized modifications or deletions. We can implement these strategies to safeguard against audit log tampering:

### 1. **Write-Only Database Rules (No UPDATE/DELETE)**
- To enforce **write-only behavior** on the `audit_log` table, **PostgreSQL rules** can be applied to block `UPDATE` and `DELETE` operations. 
- With more recent Postgres, we could even run `REVOKE UPDATE, DELETE ON audit_log FROM PUBLIC;`
### 2. Application-Level Guardrails
- Ensure that the service code never calls UPDATE or DELETE on the audit_log table.
- Implement service-level policies that enforce append-only operations.
### 3. For higher security, store hash
- Store hash of original audits and do an integrity check

## Testing Strategy

Our testing approach includes unit tests for business logic and integration tests for API validation.

1. Unit Tests (Service Layer)
   - Validate business logic in AuditLogService using Mockito.
   - Test repository interactions and edge cases (e.g., missing ACLs, unauthorized access) by mocking.
   - Process audit events from JSON (expected value).

Tools: JUnit 5, Mockito, Gson

3. Integration Tests (Controller Layer)
   - Verify API behavior using MockMvc.
   - Test authentication, log retrieval, and input validation.

Tools: Spring Boot Test, MockMvc, Embedded Kafka, h2 DB

## Deployment and Scalability Considerations

The application is packaged and deployed as follows:
- **Spring Boot WAR**: The application is packaged as a WAR file to run within an existing Tomcat server.
- **Tomcat**: Deploy The WAR file is deployed on a Tomcat server, which is hosted on a CentOS-based virtual machine.

For handling increased load:
- **Docker Containers**: The service is containerized using Docker, making it easier to manage, deploy, and scale.
- **Orchestration**: Kubernetes (or a similar container orchestration tool) would be used to scale the service horizontally by managing multiple replicas and ensuring high availability.
- **Event Streaming**:  Kafka is employed for event-driven communication, decoupling services and allowing for better scalability and fault tolerance across distributed systems.
- **Database Optimization**: The database is optimized for horizontal scaling, ensuring it can handle large volumes of audit logs without performance degradation. This includes efficient indexing and partitioning strategies.
- **Cloud-Agnostic**: The application is designed to be cloud-agnostic, meaning it can be deployed on various cloud platforms such as AWS, Azure, or Google Cloud, depending on your infrastructure needs.