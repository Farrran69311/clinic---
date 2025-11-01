package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class MeetingMinute {
    private final String id;
    private final String sessionId;
    private final LocalDateTime recordedAt;
    private final String authorDoctorId;
    private final String summary;
    private final String actionItems;

    public MeetingMinute(String id, String sessionId, LocalDateTime recordedAt, String authorDoctorId, String summary, String actionItems) {
        this.id = Objects.requireNonNull(id);
        this.sessionId = Objects.requireNonNull(sessionId);
        this.recordedAt = recordedAt;
        this.authorDoctorId = authorDoctorId;
        this.summary = summary == null ? "" : summary;
        this.actionItems = actionItems == null ? "" : actionItems;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public String getAuthorDoctorId() {
        return authorDoctorId;
    }

    public String getSummary() {
        return summary;
    }

    public String getActionItems() {
        return actionItems;
    }
}
