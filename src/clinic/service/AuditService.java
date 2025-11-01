package clinic.service;

import clinic.model.AuditLog;
import clinic.persistence.AuditLogRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog logAction(String userId,
                              String role,
                              String action,
                              String entityType,
                              String entityId,
                              String detail,
                              String result,
                              String ipAddress) throws IOException {
        AuditLog log = new AuditLog(
            java.util.UUID.randomUUID().toString(),
            LocalDateTime.now(),
            userId,
            role,
            action,
            entityType,
            entityId,
            detail,
            result,
            ipAddress
        );
        auditLogRepository.append(log);
        return log;
    }

    public List<AuditLog> listAll() throws IOException {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> filter(String keyword) throws IOException {
        return filter(log -> contains(log.getAction(), keyword)
            || contains(log.getDetail(), keyword)
            || contains(log.getRole(), keyword)
            || contains(log.getEntityType(), keyword));
    }

    public List<AuditLog> filter(Predicate<AuditLog> predicate) throws IOException {
        return auditLogRepository.findAll()
            .stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    private boolean contains(String source, String keyword) {
        return source != null && keyword != null && source.contains(keyword);
    }
}
