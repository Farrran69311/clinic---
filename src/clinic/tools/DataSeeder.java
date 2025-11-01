package clinic.tools;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Consultation;
import clinic.model.Doctor;
import clinic.model.ExpertAdvice;
import clinic.model.Patient;
import clinic.model.WorkProgress;
import clinic.service.AppointmentService;
import clinic.service.ConsultationService;
import clinic.service.ExpertAdviceService;
import clinic.service.PatientService;
import clinic.service.WorkProgressService;
import clinic.util.DoctorMatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility entry-point that seeds recent doctor activity data so weekly summaries have content.
 * This uses application services instead of manual CSV edits to keep the data flow consistent.
 */
public final class DataSeeder {
    private DataSeeder() {
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        AppContext context = new AppContext(dataDir);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        PatientService patientService = context.getPatientService();
        AppointmentService appointmentService = context.getAppointmentService();
        WorkProgressService workProgressService = context.getWorkProgressService();
        ConsultationService consultationService = context.getConsultationService();
        ExpertAdviceService expertAdviceService = context.getExpertAdviceService();

        List<Doctor> doctors = context.getDoctorService().listDoctors();
        List<Patient> patients = patientService.listPatients();
        if (patients.isEmpty()) {
            throw new IllegalStateException("No patients available to seed doctor activity");
        }

        Map<String, String> doctorDefaultPatients = buildDoctorPatientMapping(appointmentService.listAppointments(), workProgressService.listAll(), doctors, patients);

        ensureRecentWorkProgress(doctors, workProgressService, doctorDefaultPatients, start, today);
        ensureRecentConsultations(doctors, consultationService, appointmentService, doctorDefaultPatients, start, today);
        ensureRecentExpertAdvice(doctors, expertAdviceService, doctorDefaultPatients, start, today);

        System.out.println("Doctor activity seed completed at " + LocalDateTime.now());
    }

    private static Map<String, String> buildDoctorPatientMapping(List<Appointment> appointments,
                                                                 List<WorkProgress> progresses,
                                                                 List<Doctor> doctors,
                                                                 List<Patient> patients) {
        Map<String, String> map = new HashMap<>();
        // Prefer existing appointment relationships
        for (Appointment appointment : appointments) {
            map.putIfAbsent(appointment.getDoctorId(), appointment.getPatientId());
        }
        // Fall back to work progress ownerships if present
        for (WorkProgress progress : progresses) {
            map.putIfAbsent(progress.getOwnerDoctorId(), progress.getPatientId());
        }
        // Ensure every doctor has at least one mapped patient
        String fallbackPatient = patients.get(0).getId();
        for (Doctor doctor : doctors) {
            map.putIfAbsent(doctor.getId(), fallbackPatient);
        }
        return map;
    }

    private static void ensureRecentWorkProgress(List<Doctor> doctors,
                                                 WorkProgressService workProgressService,
                                                 Map<String, String> doctorDefaultPatients,
                                                 LocalDate start,
                                                 LocalDate end) throws IOException {
        List<WorkProgress> allProgress = workProgressService.listAll();
        for (Doctor doctor : doctors) {
            boolean hasRecent = allProgress.stream()
                .filter(progress -> DoctorMatcher.matches(progress.getOwnerDoctorId(), doctor))
                .anyMatch(progress -> progress.getLastUpdated() != null
                    && !progress.getLastUpdated().isBefore(start)
                    && !progress.getLastUpdated().isAfter(end));
            if (!hasRecent) {
                String patientId = doctorDefaultPatients.get(doctor.getId());
                String description = "%s随访-强化护理计划".formatted(doctor.getDepartment() == null ? "综合" : doctor.getDepartment());
                workProgressService.createProgress(
                    patientId,
                    description,
                    "IN_PROGRESS",
                    end.minusDays(1),
                    doctor.getId()
                );
            }
        }
    }

    private static void ensureRecentConsultations(List<Doctor> doctors,
                                                  ConsultationService consultationService,
                                                  AppointmentService appointmentService,
                                                  Map<String, String> doctorDefaultPatients,
                                                  LocalDate start,
                                                  LocalDate end) throws IOException {
        List<Consultation> consultations = consultationService.listConsultations();
        Map<String, List<Appointment>> doctorAppointments = appointmentService.listAppointments().stream()
            .collect(Collectors.groupingBy(Appointment::getDoctorId));
        for (Doctor doctor : doctors) {
            boolean hasRecent = consultations.stream()
                .filter(item -> DoctorMatcher.matches(item.getDoctorId(), doctor))
                .anyMatch(item -> item.getCreatedAt() != null
                    && !item.getCreatedAt().toLocalDate().isBefore(start)
                    && !item.getCreatedAt().toLocalDate().isAfter(end));
            if (!hasRecent) {
                String patientId = doctorDefaultPatients.get(doctor.getId());
                Optional<Appointment> relatedAppointment = doctorAppointments.getOrDefault(doctor.getId(), List.of()).stream()
                    .min(Comparator.comparing(Appointment::getDateTime));
                String appointmentId = relatedAppointment.map(Appointment::getId).orElse(null);
                String summary = "%s随访总结：患者症状较之前有所改善，继续当前管理方案".formatted(
                    doctor.getDepartment() == null ? "综合" : doctor.getDepartment());
                consultationService.createConsultation(
                    patientId,
                    doctor.getId(),
                    appointmentId,
                    summary,
                    null
                );
            }
        }
    }

    private static void ensureRecentExpertAdvice(List<Doctor> doctors,
                                                 ExpertAdviceService expertAdviceService,
                                                 Map<String, String> doctorDefaultPatients,
                                                 LocalDate start,
                                                 LocalDate end) throws IOException {
        List<ExpertAdvice> adviceList = expertAdviceService.listAll();
        Set<String> doctorIdsWithRecentAdvice = adviceList.stream()
            .filter(item -> item.getAdviceDate() != null
                && !item.getAdviceDate().isBefore(start)
                && !item.getAdviceDate().isAfter(end))
            .map(ExpertAdvice::getDoctorId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());
        for (Doctor doctor : doctors) {
            if (doctorIdsWithRecentAdvice.stream().anyMatch(id -> DoctorMatcher.matches(id, doctor))) {
                continue;
            }
            String patientId = doctorDefaultPatients.get(doctor.getId());
            String summary = "%s专家建议：巩固治疗方案并安排随访".formatted(
                doctor.getDepartment() == null ? "综合" : doctor.getDepartment());
            String followUp = "一周后复诊，记录关键指标变化";
            expertAdviceService.createAdvice(
                null,
                patientId,
                doctor.getId(),
                end.minusDays(2),
                summary,
                followUp
            );
        }
    }
}
