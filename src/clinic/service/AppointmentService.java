package clinic.service;

import clinic.model.Appointment;
import clinic.persistence.AppointmentRepository;
import clinic.persistence.CsvDataStore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AppointmentService {
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<Appointment> listAppointments() throws IOException {
        return appointmentRepository.findAll();
    }

    public Appointment createAppointment(String patientId, String doctorId, LocalDateTime dateTime, String notes) throws IOException {
        ensureNoConflict(doctorId, dateTime);
        Appointment appointment = new Appointment(
            CsvDataStore.randomId(),
            patientId,
            doctorId,
            dateTime,
            "PENDING",
            notes
        );
        appointmentRepository.save(appointment);
        return appointment;
    }

    public void updateStatus(String appointmentId, String status) throws IOException {
        List<Appointment> all = appointmentRepository.findAll();
        for (Appointment appointment : all) {
            if (appointment.getId().equals(appointmentId)) {
                Appointment updated = new Appointment(
                    appointment.getId(),
                    appointment.getPatientId(),
                    appointment.getDoctorId(),
                    appointment.getDateTime(),
                    status,
                    appointment.getNotes()
                );
                appointmentRepository.save(updated);
                return;
            }
        }
        throw new IllegalArgumentException("未找到预约");
    }

    public void deleteAppointment(String appointmentId) throws IOException {
        appointmentRepository.deleteById(appointmentId);
    }

    private void ensureNoConflict(String doctorId, LocalDateTime dateTime) throws IOException {
        for (Appointment existing : appointmentRepository.findAll()) {
            if (existing.getDoctorId().equals(doctorId) && existing.getDateTime().equals(dateTime)) {
                throw new IllegalArgumentException("该时间段医生已有预约");
            }
        }
    }
}
