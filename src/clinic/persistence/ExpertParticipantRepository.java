package clinic.persistence;

import clinic.model.ExpertParticipant;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExpertParticipantRepository {
    private static final String HEADER = "sessionId|participantId|participantRole";

    private final Path file;

    public ExpertParticipantRepository(Path file) {
        this.file = file;
    }

    public List<ExpertParticipant> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<ExpertParticipant> participants = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 3) {
                continue;
            }
            participants.add(new ExpertParticipant(row[0], row[1], row[2]));
        }
        return participants;
    }

    public List<ExpertParticipant> findBySessionId(String sessionId) throws IOException {
        return findAll().stream()
            .filter(p -> p.getSessionId().equals(sessionId))
            .collect(Collectors.toList());
    }

    public void replaceSessionParticipants(String sessionId, List<ExpertParticipant> participants) throws IOException {
        List<ExpertParticipant> all = findAll();
        all.removeIf(p -> p.getSessionId().equals(sessionId));
        all.addAll(participants);
        write(all);
    }

    private void write(List<ExpertParticipant> participants) throws IOException {
        List<String[]> rows = new ArrayList<>(participants.size());
        for (ExpertParticipant participant : participants) {
            rows.add(new String[]{
                participant.getSessionId(),
                participant.getParticipantId(),
                participant.getParticipantRole()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
