package clinic.service;

import clinic.model.ExpertAdvice;
import clinic.persistence.CsvDataStore;
import clinic.persistence.ExpertAdviceRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class ExpertAdviceService {
    private final ExpertAdviceRepository expertAdviceRepository;

    public ExpertAdviceService(ExpertAdviceRepository expertAdviceRepository) {
        this.expertAdviceRepository = expertAdviceRepository;
    }

    public List<ExpertAdvice> listAll() throws IOException {
        return expertAdviceRepository.findAll();
    }

    public List<ExpertAdvice> listByPatient(String patientId) throws IOException {
        return expertAdviceRepository.findByPatient(patientId);
    }

    public ExpertAdvice createAdvice(String sessionId, String patientId, String doctorId, LocalDate date, String summary, String followUp) throws IOException {
        ExpertAdvice advice = new ExpertAdvice(
            CsvDataStore.randomId(),
            sessionId,
            patientId,
            doctorId,
            date,
            summary,
            followUp
        );
        expertAdviceRepository.save(advice);
        return advice;
    }

    public void updateAdvice(ExpertAdvice advice) throws IOException {
        expertAdviceRepository.save(advice);
    }

    public void deleteAdvice(String id) throws IOException {
        expertAdviceRepository.deleteById(id);
    }
}
