package clinic.tools;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Doctor;
import clinic.model.ExpertSession;
import clinic.model.Patient;
import clinic.model.User;
import clinic.service.AppointmentService;
import clinic.service.AuthService;
import clinic.service.CalendarEventService;
import clinic.service.CaseRecordService;
import clinic.service.ConsultationService;
import clinic.service.DoctorService;
import clinic.service.ExpertAdviceService;
import clinic.service.ExpertSessionService;
import clinic.service.MeetingMinuteService;
import clinic.service.PatientService;
import clinic.service.PharmacyService;
import clinic.service.WorkProgressService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Utility class to expand the CSV data set using the application services instead of manual edits.
 * It ensures every CSV has at least {@value #TARGET_COUNT} records while keeping IDs and relations consistent.
 */
public final class BulkDataSeeder {
    private static final int TARGET_COUNT = 30;
    private static final String PATIENT_USERNAME_PREFIX = "patient_auto_";
    private static final String DOCTOR_USERNAME_PREFIX = "doctor_auto_";
    private static final String DEFAULT_PASSWORD = "clinic123";

    private final AppContext context;
    private final Random random = new Random(20251101L);

    private BulkDataSeeder(AppContext context) {
        this.context = context;
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        BulkDataSeeder seeder = new BulkDataSeeder(new AppContext(dataDir));
        seeder.seedAll();
        System.out.println("Bulk data seed completed.");
    }

    private void seedAll() throws IOException {
        ensurePatients();
        ensureDoctors();
        ensureAppointments();
        ensureCaseLibrary();
        ensureWorkProgress();
        ensureCalendarEvents();
        ensureExpertSessionsAndParticipants();
        ensureExpertAdvices();
        ensureConsultations();
        ensureMedicines();
        ensureMeetingMinutes();
        ensurePrescriptions();
    }

    private void ensurePatients() throws IOException {
        PatientService patientService = context.getPatientService();
        AuthService authService = context.getAuthService();
        List<Patient> patients = patientService.listPatients();
        int index = patients.size();
        while (patients.size() < TARGET_COUNT) {
            index++;
            String username = PATIENT_USERNAME_PREFIX + index;
            if (authService.userExists(username)) {
                continue;
            }
            User user = authService.registerPatient(username, DEFAULT_PASSWORD);
            LocalDate birthday = LocalDate.of(1980 + (index % 25), ((index - 1) % 12) + 1, Math.max(1, (index % 27) + 1));
            String gender = index % 2 == 0 ? "男" : "女";
            String phone = String.format(Locale.CHINA, "13%09d", 100_000_000 + index * 73 % 900_000_000);
            String address = "华夏市" + (index % 10 + 1) + "区健康大道" + (index % 20 + 10) + "号";
            String contact = "亲属 " + (phone.substring(0, 3) + "***" + phone.substring(7));
            String notes = "随访重点: " + (index % 2 == 0 ? "慢病管理" : "心理辅导");
            Patient enriched = new Patient(
                user.getId(),
                "患者" + index,
                gender,
                birthday,
                phone,
                address,
                contact,
                notes
            );
            patientService.updatePatient(enriched);
            patients = patientService.listPatients();
        }
    }

    private void ensureDoctors() throws IOException {
        DoctorService doctorService = context.getDoctorService();
        AuthService authService = context.getAuthService();
        List<Doctor> doctors = doctorService.listDoctors();
        int index = doctors.size();
        while (doctors.size() < TARGET_COUNT) {
            index++;
            String name = "专家" + index + "医生";
            String department = DEPARTMENTS[index % DEPARTMENTS.length];
            String phone = String.format(Locale.CHINA, "010-%08d", 20000000 + (index * 137) % 8000000);
            String schedule = "周" + WEEKDAYS[index % WEEKDAYS.length] + " " + (8 + index % 5) + ":00-" + (13 + index % 5) + ":30";
            double rating = 4.0 + (index % 10) / 10.0;
            String title = TITLES[index % TITLES.length];
            String level = LEVELS[index % LEVELS.length];
            String specialties = SPECIALTIES[index % SPECIALTIES.length];
            doctorService.createDoctor(name, department, phone, schedule, rating, title, level, specialties);
            String username = DOCTOR_USERNAME_PREFIX + index;
            if (!authService.userExists(username)) {
                authService.createDoctorAccount(username, DEFAULT_PASSWORD);
            }
            doctors = doctorService.listDoctors();
        }
    }

    private void ensureAppointments() throws IOException {
        AppointmentService appointmentService = context.getAppointmentService();
        List<Appointment> appointments = appointmentService.listAppointments();
        if (appointments.size() >= TARGET_COUNT) {
            return;
        }
        List<Patient> patients = context.getPatientService().listPatients();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        Map<String, Set<LocalDateTime>> occupiedSlots = new HashMap<>();
        for (Appointment appointment : appointments) {
            occupiedSlots.computeIfAbsent(appointment.getDoctorId(), k -> new HashSet<>()).add(appointment.getDateTime());
        }
        int attempts = 0;
        while (appointments.size() < TARGET_COUNT && attempts < TARGET_COUNT * 10) {
            attempts++;
            Patient patient = patients.get(random.nextInt(patients.size())) ;
            Doctor doctor = doctors.get(random.nextInt(doctors.size()));
            LocalDateTime slot = findAvailableSlot(occupiedSlots.computeIfAbsent(doctor.getId(), k -> new HashSet<>()));
            try {
                Appointment created = appointmentService.createAppointment(
                    patient.getId(),
                    doctor.getId(),
                    slot,
                    "定期复诊评估" + (appointments.size() + 1)
                );
                String status = APPOINTMENT_STATUS[(appointments.size() + attempts) % APPOINTMENT_STATUS.length];
                appointmentService.updateStatus(created.getId(), status);
                appointments = appointmentService.listAppointments();
            } catch (IllegalArgumentException ex) {
                // retry with a different slot
            }
        }
    }

    private LocalDateTime findAvailableSlot(Set<LocalDateTime> occupied) {
        LocalDate baseDate = LocalDate.now().plusDays(random.nextInt(14));
        int hour = 8 + random.nextInt(8);
        int minute = (random.nextInt(4)) * 15;
        LocalDateTime candidate = LocalDateTime.of(baseDate, LocalTime.of(hour, minute));
        int safety = 0;
        while (occupied.contains(candidate) && safety < 20) {
            candidate = candidate.plusMinutes(15);
            safety++;
        }
        occupied.add(candidate);
        return candidate;
    }

    private void ensureCaseLibrary() throws IOException {
        CaseRecordService caseRecordService = context.getCaseRecordService();
        int count = caseRecordService.listAll().size();
        List<Patient> patients = context.getPatientService().listPatients();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Patient patient = patients.get(index % patients.size());
            String title = "病例解读" + index;
            String summary = "针对患者" + patient.getName() + " 的阶段性诊疗总结与随访建议。";
            String tags = CASE_TAGS[index % CASE_TAGS.length];
            String attachment = "report_" + index + ".pdf";
            caseRecordService.createCaseRecord(
                patient.getId(),
                title,
                summary,
                tags,
                attachment
            );
            count = caseRecordService.listAll().size();
        }
    }

    private void ensureWorkProgress() throws IOException {
        WorkProgressService workProgressService = context.getWorkProgressService();
        int count = workProgressService.listAll().size();
        List<Patient> patients = context.getPatientService().listPatients();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Patient patient = patients.get(index % patients.size());
            Doctor doctor = doctors.get(index % doctors.size());
            LocalDate lastUpdated = LocalDate.now().minusDays(random.nextInt(6));
            String status = PROGRESS_STATUS[index % PROGRESS_STATUS.length];
            String description = doctor.getDepartment() + "随访-阶段任务" + index;
            workProgressService.createProgress(
                patient.getId(),
                description,
                status,
                lastUpdated,
                doctor.getId()
            );
            count = workProgressService.listAll().size();
        }
    }

    private void ensureCalendarEvents() throws IOException {
        CalendarEventService calendarEventService = context.getCalendarEventService();
        int count = calendarEventService.listAll().size();
        List<Patient> patients = context.getPatientService().listPatients();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Doctor doctor = doctors.get(index % doctors.size());
            Patient patient = patients.get(index * 3 % patients.size());
            LocalDateTime start = LocalDateTime.now().plusDays(index % 15).withHour(9 + index % 6).withMinute(0);
            LocalDateTime end = start.plusMinutes(45);
            String title = doctor.getDepartment() + " 随访安排" + index;
            String notes = "随访重点: " + patient.getName() + " 的阶段总结";
            calendarEventService.createEvent(
                title,
                start,
                end,
                patient.getId(),
                doctor.getId(),
                "门诊" + (index % 5 + 1) + "室",
                notes
            );
            count = calendarEventService.listAll().size();
        }
    }

    private void ensureExpertSessionsAndParticipants() throws IOException {
        ExpertSessionService sessionService = context.getExpertSessionService();
        int count = sessionService.listSessions().size();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        List<Patient> patients = context.getPatientService().listPatients();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Doctor host = doctors.get(index % doctors.size());
            LocalDateTime scheduled = LocalDateTime.now().plusDays(index % 20).withHour(14 + index % 4);
            String title = host.getDepartment() + " 专家讨论会 " + index;
            String status = SESSION_STATUS[index % SESSION_STATUS.length];
            String url = "https://meet.example.com/session/" + index;
            ExpertSession session = sessionService.createSession(
                title,
                host.getId(),
                scheduled,
                status,
                url,
                "聚焦复杂病例的会诊与方案制定"
            );
            sessionService.addParticipant(session.getId(), host.getId(), "主持人");
            Doctor peer = doctors.get((index + 5) % doctors.size());
            sessionService.addParticipant(session.getId(), peer.getId(), "协同专家");
            Patient patient = patients.get((index * 2) % patients.size());
            sessionService.addParticipant(session.getId(), patient.getId(), "病例患者");
            count = sessionService.listSessions().size();
        }
    }

    private void ensureExpertAdvices() throws IOException {
        ExpertAdviceService adviceService = context.getExpertAdviceService();
        int count = adviceService.listAll().size();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        List<Patient> patients = context.getPatientService().listPatients();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Doctor doctor = doctors.get(index % doctors.size());
            Patient patient = patients.get((index * 7) % patients.size());
            LocalDate adviceDate = LocalDate.now().minusDays(random.nextInt(7));
            String summary = doctor.getDepartment() + " 专家建议第 " + index + " 条";
            String follow = "复诊计划: " + adviceDate.plusDays(7) + " 回院评估";
            adviceService.createAdvice(
                null,
                patient.getId(),
                doctor.getId(),
                adviceDate,
                summary,
                follow
            );
            count = adviceService.listAll().size();
        }
    }

    private void ensureConsultations() throws IOException {
        ConsultationService consultationService = context.getConsultationService();
        int count = consultationService.listConsultations().size();
        List<Appointment> appointments = context.getAppointmentService().listAppointments();
        List<Patient> patients = context.getPatientService().listPatients();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            Appointment appointment = appointments.get(index % appointments.size());
            Patient patient = patients.stream()
                .filter(p -> p.getId().equals(appointment.getPatientId()))
                .findFirst()
                .orElse(patients.get(index % patients.size()));
            Doctor doctor = doctors.stream()
                .filter(d -> d.getId().equals(appointment.getDoctorId()))
                .findFirst()
                .orElse(doctors.get(index % doctors.size()));
            String summary = "问诊总结" + index + "：患者" + patient.getName() + " 状态评估完成。";
            consultationService.createConsultation(
                patient.getId(),
                doctor.getId(),
                appointment.getId(),
                summary,
                null
            );
            count = consultationService.listConsultations().size();
        }
    }

    private void ensureMedicines() throws IOException {
        PharmacyService pharmacyService = context.getPharmacyService();
        int count = pharmacyService.listMedicines().size();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            String name = "复方制剂" + index;
            String spec = (index % 2 == 0 ? "片剂" : "针剂") + ", 0." + (index % 5 + 1) + "g";
            int stock = 100 + (index * 7) % 200;
            String unit = index % 2 == 0 ? "盒" : "支";
            LocalDate expiry = LocalDate.now().plusMonths(6 + index % 18);
            pharmacyService.addMedicine(name, spec, stock, unit, expiry);
            count = pharmacyService.listMedicines().size();
        }
    }

    private void ensureMeetingMinutes() throws IOException {
        MeetingMinuteService meetingMinuteService = context.getMeetingMinuteService();
        int count = meetingMinuteService.listAll().size();
        List<ExpertSession> sessions = context.getExpertSessionService().listSessions();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            ExpertSession session = sessions.get(index % sessions.size());
            Doctor doctor = doctors.get((index * 11) % doctors.size());
            String summary = "会议纪要" + index + "：总结重点讨论要点与分工。";
            String actions = "行动项" + index + "：安排相关检查与复诊。";
            meetingMinuteService.createMinute(session.getId(), doctor.getId(), summary, actions);
            count = meetingMinuteService.listAll().size();
        }
    }

    private void ensurePrescriptions() throws IOException {
        PharmacyService pharmacyService = context.getPharmacyService();
        int count = pharmacyService.listPrescriptions().size();
        List<String> medicineIds = pharmacyService.listMedicines().stream().map(m -> m.getId()).toList();
        List<String> consultationIds = context.getConsultationService().listConsultations().stream().map(c -> c.getId()).toList();
        int index = count;
        while (count < TARGET_COUNT) {
            index++;
            String consultationId = consultationIds.get(index % consultationIds.size());
            String medicineId = medicineIds.get((index * 5) % medicineIds.size());
            int quantity = 1 + (index % 5);
            String usage = "每日" + (index % 3 + 1) + "次，每次" + (index % 2 + 1) + "片";
            pharmacyService.createPrescription(consultationId, medicineId, quantity, usage);
            count = pharmacyService.listPrescriptions().size();
        }
    }

    private static final String[] DEPARTMENTS = {
        "全科", "呼吸科", "儿科", "内分泌科", "心内科", "骨科", "皮肤科", "神经内科", "肿瘤科", "康复科"
    };

    private static final String[] TITLES = {
        "主任医师", "副主任医师", "主治医师", "住院医师"
    };

    private static final String[] LEVELS = {
        "正高级", "副高级", "中级", "初级"
    };

    private static final String[] SPECIALTIES = {
        "慢病管理", "疑难杂症会诊", "精准用药", "术后康复", "心理支持", "儿童保健", "心血管调控", "肿瘤营养", "睡眠医学", "皮肤免疫"
    };

    private static final String[] WEEKDAYS = {
        "一", "二", "三", "四", "五", "六", "日"
    };

    private static final String[] APPOINTMENT_STATUS = {
        "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"
    };

    private static final String[] CASE_TAGS = {
        "慢病管理", "远程监测", "术后康复", "营养随访", "心理干预", "物理治疗", "免疫调节", "心脑血管", "呼吸系统", "儿科管理"
    };

    private static final String[] PROGRESS_STATUS = {
        "PENDING", "IN_PROGRESS", "DONE"
    };

    private static final String[] SESSION_STATUS = {
        "PLANNED", "SCHEDULED", "COMPLETED"
    };
}
