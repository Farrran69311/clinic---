package clinic.service;

import clinic.model.Doctor;
import clinic.persistence.CsvDataStore;
import clinic.persistence.DoctorRepository;

import java.io.IOException;
import java.util.List;

public class DoctorService {
    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<Doctor> listDoctors() throws IOException {
        return doctorRepository.findAll();
    }

    public Doctor createDoctor(String name, String department, String phone, String schedule) throws IOException {
        return createDoctor(name, department, phone, schedule, null, null, null, null);
    }

    public Doctor createDoctor(String name, String department, String phone, String schedule,
                               Double rating, String title, String level, String specialties) throws IOException {
        Doctor doctor = new Doctor(
            CsvDataStore.randomId(),
            name,
            department,
            phone,
            schedule,
            rating,
            title,
            level,
            specialties
        );
        doctorRepository.save(doctor);
        return doctor;
    }

    public void updateDoctor(Doctor doctor) throws IOException {
        doctorRepository.save(doctor);
    }

    public void deleteDoctor(String id) throws IOException {
        doctorRepository.deleteById(id);
    }
}
