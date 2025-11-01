package clinic.model;

import java.util.Objects;

public class Prescription {
    private final String id;
    private final String consultationId;
    private final String medicineId;
    private final int quantity;
    private final String usage;
    private final String status;

    public Prescription(String id, String consultationId, String medicineId, int quantity, String usage, String status) {
        this.id = Objects.requireNonNull(id);
        this.consultationId = Objects.requireNonNull(consultationId);
        this.medicineId = Objects.requireNonNull(medicineId);
        this.quantity = Math.max(quantity, 0);
        this.usage = usage == null ? "" : usage;
        this.status = status == null ? "PENDING" : status;
    }

    public String getId() {
        return id;
    }

    public String getConsultationId() {
        return consultationId;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUsage() {
        return usage;
    }

    public String getStatus() {
        return status;
    }
}
