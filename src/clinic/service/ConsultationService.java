package clinic.service;

import clinic.model.Consultation;
import clinic.persistence.ConsultationRepository;
import clinic.persistence.CsvDataStore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ConsultationService {
    private final ConsultationRepository consultationRepository;

    public ConsultationService(ConsultationRepository consultationRepository) {
        this.consultationRepository = consultationRepository;
    }

    public List<Consultation> listConsultations() throws IOException {
        return consultationRepository.findAll();
    }

    public List<Consultation> listByPatient(String patientId) throws IOException {
        return consultationRepository.findAll().stream()
            .filter(c -> c.getPatientId().equals(patientId))
            .collect(Collectors.toList());
    }

    public Consultation createConsultation(String patientId, String doctorId, String appointmentId, String summary, String prescriptionId) throws IOException {
        Consultation consultation = new Consultation(
            CsvDataStore.randomId(),
            patientId,
            doctorId,
            appointmentId,
            summary,
            prescriptionId,
            LocalDateTime.now()
        );
        consultationRepository.save(consultation);
        return consultation;
    }

    public void updateConsultation(Consultation consultation) throws IOException {
        consultationRepository.save(consultation);
    }

    public void deleteConsultation(String id) throws IOException {
        consultationRepository.deleteById(id);
    }
}
