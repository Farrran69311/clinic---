package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class CalendarEvent {
    private final String id;
    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String relatedPatientId;
    private final String ownerDoctorId;
    private final String location;
    private final String notes;

    public CalendarEvent(String id, String title, LocalDateTime start, LocalDateTime end, String relatedPatientId, String ownerDoctorId, String location, String notes) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.start = start;
        this.end = end;
        this.relatedPatientId = relatedPatientId;
        this.ownerDoctorId = ownerDoctorId;
        this.location = location;
        this.notes = notes == null ? "" : notes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public String getRelatedPatientId() {
        return relatedPatientId;
    }

    public String getOwnerDoctorId() {
        return ownerDoctorId;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }
}
