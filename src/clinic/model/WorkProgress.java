package clinic.model;

import java.time.LocalDate;
import java.util.Objects;

public class WorkProgress {
    private final String id;
    private final String patientId;
    private final String description;
    private final String status;
    private final LocalDate lastUpdated;
    private final String ownerDoctorId;

    public WorkProgress(String id, String patientId, String description, String status, LocalDate lastUpdated, String ownerDoctorId) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.description = Objects.requireNonNull(description);
        this.status = status == null ? "ONGOING" : status;
        this.lastUpdated = lastUpdated;
        this.ownerDoctorId = ownerDoctorId;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getLastUpdated() {
        return lastUpdated;
    }

    public String getOwnerDoctorId() {
        return ownerDoctorId;
    }
}
