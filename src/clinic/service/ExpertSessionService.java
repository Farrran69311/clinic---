package clinic.service;

import clinic.model.ExpertParticipant;
import clinic.model.ExpertSession;
import clinic.persistence.CsvDataStore;
import clinic.persistence.ExpertParticipantRepository;
import clinic.persistence.ExpertSessionRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExpertSessionService {
    private final ExpertSessionRepository sessionRepository;
    private final ExpertParticipantRepository participantRepository;

    public ExpertSessionService(ExpertSessionRepository sessionRepository, ExpertParticipantRepository participantRepository) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
    }

    public List<ExpertSession> listSessions() throws IOException {
        return sessionRepository.findAll();
    }

    public ExpertSession createSession(String title, String hostDoctorId, LocalDateTime scheduledAt, String status, String meetingUrl, String notes) throws IOException {
        ExpertSession session = new ExpertSession(
            CsvDataStore.randomId(),
            title,
            hostDoctorId,
            scheduledAt,
            status,
            meetingUrl,
            notes
        );
        sessionRepository.save(session);
        return session;
    }

    public void updateSession(ExpertSession session) throws IOException {
        sessionRepository.save(session);
    }

    public void deleteSession(String id) throws IOException {
        sessionRepository.deleteById(id);
        participantRepository.replaceSessionParticipants(id, new ArrayList<>());
    }

    public List<ExpertParticipant> listParticipants(String sessionId) throws IOException {
        return participantRepository.findBySessionId(sessionId);
    }

    public void setParticipants(String sessionId, List<ExpertParticipant> participants) throws IOException {
        participantRepository.replaceSessionParticipants(sessionId, participants);
    }

    public void addParticipant(String sessionId, String participantId, String role) throws IOException {
        List<ExpertParticipant> participants = new ArrayList<>(participantRepository.findBySessionId(sessionId));
        participants.add(new ExpertParticipant(sessionId, participantId, role));
        participantRepository.replaceSessionParticipants(sessionId, participants);
    }

    public void removeParticipant(String sessionId, String participantId) throws IOException {
        List<ExpertParticipant> participants = new ArrayList<>(participantRepository.findBySessionId(sessionId));
        participants.removeIf(p -> p.getParticipantId().equals(participantId));
        participantRepository.replaceSessionParticipants(sessionId, participants);
    }
}
