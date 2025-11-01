package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Consultation {
    private final String id;
    private final String patientId;
    private final String doctorId;
    private final String appointmentId;
    private final String summary;
    private final String prescriptionId;
    private final LocalDateTime createdAt;

    public Consultation(String id, String patientId, String doctorId, String appointmentId, String summary, String prescriptionId, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.appointmentId = appointmentId;
        this.summary = summary == null ? "" : summary;
        this.prescriptionId = prescriptionId;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getSummary() {
        return summary;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
