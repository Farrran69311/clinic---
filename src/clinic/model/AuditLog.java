package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class AuditLog {
    private final String id;
    private final LocalDateTime timestamp;
    private final String userId;
    private final String role;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final String detail;
    private final String result;
    private final String ipAddress;

    public AuditLog(String id,
                    LocalDateTime timestamp,
                    String userId,
                    String role,
                    String action,
                    String entityType,
                    String entityId,
                    String detail,
                    String result,
                    String ipAddress) {
        this.id = Objects.requireNonNull(id);
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.userId = userId;
        this.role = role;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail == null ? "" : detail;
        this.result = result == null ? "SUCCESS" : result;
        this.ipAddress = ipAddress;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetail() {
        return detail;
    }

    public String getResult() {
        return result;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
