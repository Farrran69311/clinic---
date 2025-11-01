package clinic.model;

import java.util.Objects;

public class ExpertParticipant {
    private final String sessionId;
    private final String participantId;
    private final String participantRole;

    public ExpertParticipant(String sessionId, String participantId, String participantRole) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.participantId = Objects.requireNonNull(participantId);
        this.participantRole = participantRole == null ? "DOCTOR" : participantRole;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getParticipantRole() {
        return participantRole;
    }
}
