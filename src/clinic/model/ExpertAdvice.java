package clinic.model;

import java.time.LocalDate;
import java.util.Objects;

public class ExpertAdvice {
    private final String id;
    private final String sessionId;
    private final String patientId;
    private final String doctorId;
    private final LocalDate adviceDate;
    private final String adviceSummary;
    private final String followUpPlan;

    public ExpertAdvice(String id, String sessionId, String patientId, String doctorId, LocalDate adviceDate, String adviceSummary, String followUpPlan) {
        this.id = Objects.requireNonNull(id);
        this.sessionId = sessionId;
        this.patientId = Objects.requireNonNull(patientId);
        this.doctorId = doctorId;
        this.adviceDate = adviceDate;
        this.adviceSummary = adviceSummary == null ? "" : adviceSummary;
        this.followUpPlan = followUpPlan == null ? "" : followUpPlan;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDate getAdviceDate() {
        return adviceDate;
    }

    public String getAdviceSummary() {
        return adviceSummary;
    }

    public String getFollowUpPlan() {
        return followUpPlan;
    }
}
