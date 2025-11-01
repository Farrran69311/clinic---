package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Appointment {
    private final String id;
    private final String patientId;
    private final String doctorId;
    private final LocalDateTime dateTime;
    private final String status;
    private final String notes;

    public Appointment(String id, String patientId, String doctorId, LocalDateTime dateTime, String status, String notes) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.dateTime = Objects.requireNonNull(dateTime);
        this.status = status == null ? "PENDING" : status;
        this.notes = notes == null ? "" : notes;
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
}
