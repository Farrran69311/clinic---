package clinic.service;

import clinic.model.Patient;
import clinic.persistence.CsvDataStore;
import clinic.persistence.PatientRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> listPatients() throws IOException {
        return patientRepository.findAll();
    }

    public Patient createPatient(String name, String gender, LocalDate birthday, String phone, String address, String emergencyContact, String notes) throws IOException {
        Patient patient = new Patient(
            CsvDataStore.randomId(),
            name,
            gender,
            birthday,
            phone,
            address,
            emergencyContact,
            notes
        );
        patientRepository.save(patient);
        return patient;
    }

    public void updatePatient(Patient patient) throws IOException {
        patientRepository.save(patient);
    }

    public void deletePatient(String id) throws IOException {
        patientRepository.deleteById(id);
    }
}
