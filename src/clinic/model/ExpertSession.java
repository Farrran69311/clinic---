package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ExpertSession {
    private final String id;
    private final String title;
    private final String hostDoctorId;
    private final LocalDateTime scheduledAt;
    private final String status;
    private final String meetingUrl;
    private final String notes;

    public ExpertSession(String id, String title, String hostDoctorId, LocalDateTime scheduledAt, String status, String meetingUrl, String notes) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.hostDoctorId = Objects.requireNonNull(hostDoctorId);
        this.scheduledAt = scheduledAt;
        this.status = status == null ? "SCHEDULED" : status;
        this.meetingUrl = meetingUrl;
        this.notes = notes == null ? "" : notes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getHostDoctorId() {
        return hostDoctorId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public String getNotes() {
        return notes;
    }
}
