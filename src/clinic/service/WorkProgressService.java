package clinic.service;

import clinic.model.WorkProgress;
import clinic.persistence.CsvDataStore;
import clinic.persistence.WorkProgressRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class WorkProgressService {
    private final WorkProgressRepository workProgressRepository;

    public WorkProgressService(WorkProgressRepository workProgressRepository) {
        this.workProgressRepository = workProgressRepository;
    }

    public List<WorkProgress> listAll() throws IOException {
        return workProgressRepository.findAll();
    }

    public List<WorkProgress> listByPatient(String patientId) throws IOException {
        return workProgressRepository.findByPatient(patientId);
    }

    public WorkProgress createProgress(String patientId, String description, String status, LocalDate lastUpdated, String ownerDoctorId) throws IOException {
        WorkProgress progress = new WorkProgress(
            CsvDataStore.randomId(),
            patientId,
            description,
            status,
            lastUpdated,
            ownerDoctorId
        );
        workProgressRepository.save(progress);
        return progress;
    }

    public void updateProgress(WorkProgress progress) throws IOException {
        workProgressRepository.save(progress);
    }

    public void deleteProgress(String id) throws IOException {
        workProgressRepository.deleteById(id);
    }
}
