package clinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class InsuranceClaim {
    public enum Status {
        PENDING,
        SUBMITTED,
        APPROVED,
        REJECTED,
        PAID
    }

    private final String id;
    private final String paymentId;
    private final String insuranceType;
    private final BigDecimal coverageRatio;
    private final BigDecimal claimedAmount;
    private final BigDecimal approvedAmount;
    private final Status status;
    private final LocalDateTime submittedAt;
    private final LocalDateTime processedAt;
    private final String notes;

    public InsuranceClaim(String id,
                          String paymentId,
                          String insuranceType,
                          BigDecimal coverageRatio,
                          BigDecimal claimedAmount,
                          BigDecimal approvedAmount,
                          Status status,
                          LocalDateTime submittedAt,
                          LocalDateTime processedAt,
                          String notes) {
        this.id = Objects.requireNonNull(id);
        this.paymentId = Objects.requireNonNull(paymentId);
        this.insuranceType = insuranceType == null ? "URBAN_EMPLOYEE" : insuranceType;
        this.coverageRatio = coverageRatio == null ? BigDecimal.ZERO : coverageRatio;
        this.claimedAmount = claimedAmount == null ? BigDecimal.ZERO : claimedAmount;
        this.approvedAmount = approvedAmount;
        this.status = status == null ? Status.PENDING : status;
        this.submittedAt = submittedAt;
        this.processedAt = processedAt;
        this.notes = notes == null ? "" : notes;
    }

    public String getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public BigDecimal getCoverageRatio() {
        return coverageRatio;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getNotes() {
        return notes;
    }

    public InsuranceClaim withStatus(Status newStatus, BigDecimal approvedAmount, LocalDateTime processedAt, String notes) {
        return new InsuranceClaim(
            id,
            paymentId,
            insuranceType,
            coverageRatio,
            claimedAmount,
            approvedAmount,
            newStatus,
            submittedAt,
            processedAt,
            notes
        );
    }
}
