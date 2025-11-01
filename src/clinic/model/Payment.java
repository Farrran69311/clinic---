package clinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Payment {
    public enum RelatedType {
        APPOINTMENT,
        PRESCRIPTION,
        OTHER
    }

    public enum Status {
        PENDING,
        PROCESSING,
        PAID,
        FAILED,
        REFUNDED
    }

    private final String id;
    private final String patientId;
    private final RelatedType relatedType;
    private final String relatedId;
    private final BigDecimal amount;
    private final String currency;
    private final String method;
    private final Status status;
    private final String insuranceClaimId;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;

    public Payment(String id,
                   String patientId,
                   RelatedType relatedType,
                   String relatedId,
                   BigDecimal amount,
                   String currency,
                   String method,
                   Status status,
                   String insuranceClaimId,
                   LocalDateTime createdAt,
                   LocalDateTime paidAt) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.relatedType = Objects.requireNonNull(relatedType);
        this.relatedId = relatedId;
        this.amount = Objects.requireNonNull(amount);
        this.currency = currency == null ? "CNY" : currency;
        this.method = method == null ? "CASH" : method;
        this.status = status == null ? Status.PENDING : status;
        this.insuranceClaimId = insuranceClaimId;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.paidAt = paidAt;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public RelatedType getRelatedType() {
        return relatedType;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMethod() {
        return method;
    }

    public Status getStatus() {
        return status;
    }

    public String getInsuranceClaimId() {
        return insuranceClaimId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Payment withStatus(Status newStatus, LocalDateTime paidAt) {
        return new Payment(
            id,
            patientId,
            relatedType,
            relatedId,
            amount,
            currency,
            method,
            newStatus,
            insuranceClaimId,
            createdAt,
            paidAt
        );
    }

    public Payment withInsuranceClaim(String claimId) {
        return new Payment(
            id,
            patientId,
            relatedType,
            relatedId,
            amount,
            currency,
            method,
            status,
            claimId,
            createdAt,
            paidAt
        );
    }
}
