package clinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class StockMovement {
    public enum MovementType {
        INBOUND,
        OUTBOUND,
        ADJUSTMENT
    }

    private final String id;
    private final String medicineId;
    private final MovementType movementType;
    private final int quantity;
    private final BigDecimal unitCost;
    private final BigDecimal totalCost;
    private final LocalDateTime occurredAt;
    private final String referenceType;
    private final String referenceId;
    private final String operatorId;
    private final String notes;

    public StockMovement(String id,
                         String medicineId,
                         MovementType movementType,
                         int quantity,
                         BigDecimal unitCost,
                         BigDecimal totalCost,
                         LocalDateTime occurredAt,
                         String referenceType,
                         String referenceId,
                         String operatorId,
                         String notes) {
        this.id = Objects.requireNonNull(id);
        this.medicineId = Objects.requireNonNull(medicineId);
        this.movementType = Objects.requireNonNull(movementType);
        this.quantity = quantity;
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        this.totalCost = totalCost == null ? this.unitCost.multiply(BigDecimal.valueOf(quantity)) : totalCost;
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.operatorId = operatorId;
        this.notes = notes == null ? "" : notes;
    }

    public String getId() {
        return id;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getNotes() {
        return notes;
    }
}
