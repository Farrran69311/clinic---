package clinic.persistence;

import clinic.model.ExpertAdvice;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExpertAdviceRepository {
    private static final String HEADER = "id|sessionId|patientId|doctorId|adviceDate|adviceSummary|followUpPlan";

    private final Path file;

    public ExpertAdviceRepository(Path file) {
        this.file = file;
    }

    public List<ExpertAdvice> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<ExpertAdvice> advices = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 7) {
                continue;
            }
            advices.add(new ExpertAdvice(
                row[0],
                row[1],
                row[2],
                row[3],
                row[4].isEmpty() ? null : LocalDate.parse(row[4]),
                row[5],
                row[6]
            ));
        }
        return advices;
    }

    public List<ExpertAdvice> findByPatient(String patientId) throws IOException {
        return findAll().stream()
            .filter(a -> a.getPatientId().equals(patientId))
            .collect(Collectors.toList());
    }

    public void save(ExpertAdvice advice) throws IOException {
        List<ExpertAdvice> advices = findAll();
        boolean updated = false;
        for (int i = 0; i < advices.size(); i++) {
            if (advices.get(i).getId().equals(advice.getId())) {
                advices.set(i, advice);
                updated = true;
                break;
            }
        }
        if (!updated) {
            advices.add(advice);
        }
        write(advices);
    }

    public void deleteById(String id) throws IOException {
        List<ExpertAdvice> advices = findAll();
        advices.removeIf(a -> a.getId().equals(id));
        write(advices);
    }

    private void write(List<ExpertAdvice> advices) throws IOException {
        List<String[]> rows = new ArrayList<>(advices.size());
        for (ExpertAdvice advice : advices) {
            rows.add(new String[]{
                advice.getId(),
                advice.getSessionId() == null ? "" : advice.getSessionId(),
                advice.getPatientId(),
                advice.getDoctorId() == null ? "" : advice.getDoctorId(),
                advice.getAdviceDate() == null ? "" : advice.getAdviceDate().toString(),
                advice.getAdviceSummary(),
                advice.getFollowUpPlan()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
