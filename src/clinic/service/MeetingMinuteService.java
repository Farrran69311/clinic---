package clinic.service;

import clinic.model.MeetingMinute;
import clinic.persistence.CsvDataStore;
import clinic.persistence.MeetingMinuteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class MeetingMinuteService {
    private final MeetingMinuteRepository meetingMinuteRepository;

    public MeetingMinuteService(MeetingMinuteRepository meetingMinuteRepository) {
        this.meetingMinuteRepository = meetingMinuteRepository;
    }

    public List<MeetingMinute> listAll() throws IOException {
        return meetingMinuteRepository.findAll();
    }

    public List<MeetingMinute> listBySession(String sessionId) throws IOException {
        return meetingMinuteRepository.findBySession(sessionId);
    }

    public MeetingMinute createMinute(String sessionId, String authorDoctorId, String summary, String actionItems) throws IOException {
        MeetingMinute minute = new MeetingMinute(
            CsvDataStore.randomId(),
            sessionId,
            LocalDateTime.now(),
            authorDoctorId,
            summary,
            actionItems
        );
        meetingMinuteRepository.save(minute);
        return minute;
    }

    public void updateMinute(MeetingMinute minute) throws IOException {
        meetingMinuteRepository.save(minute);
    }

    public void deleteMinute(String id) throws IOException {
        meetingMinuteRepository.deleteById(id);
    }
}
