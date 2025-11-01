package clinic.model;

import java.util.Objects;

public class CaseRecord {
    private final String id;
    private final String patientId;
    private final String title;
    private final String summary;
    private final String tags;
    private final String attachmentPath;

    public CaseRecord(String id, String patientId, String title, String summary, String tags, String attachmentPath) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.title = Objects.requireNonNull(title);
        this.summary = summary == null ? "" : summary;
        this.tags = tags == null ? "" : tags;
        this.attachmentPath = attachmentPath;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getTags() {
        return tags;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }
}
