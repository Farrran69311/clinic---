package clinic;

import clinic.persistence.AppointmentRepository;
import clinic.persistence.CaseRecordRepository;
import clinic.persistence.CalendarEventRepository;
import clinic.persistence.ConsultationRepository;
import clinic.persistence.DoctorRepository;
import clinic.persistence.ExpertAdviceRepository;
import clinic.persistence.ExpertParticipantRepository;
import clinic.persistence.ExpertSessionRepository;
import clinic.persistence.MedicineRepository;
import clinic.persistence.MeetingMinuteRepository;
import clinic.persistence.PatientRepository;
import clinic.persistence.PrescriptionRepository;
import clinic.persistence.UserRepository;
import clinic.persistence.WorkProgressRepository;
import clinic.service.AppointmentService;
import clinic.service.AuthService;
import clinic.service.CalendarEventService;
import clinic.service.CaseRecordService;
import clinic.service.ConsultationService;
import clinic.service.DoctorService;
import clinic.service.ExpertAdviceService;
import clinic.service.ExpertSessionService;
import clinic.service.InsightService;
import clinic.service.MeetingMinuteService;
import clinic.service.PatientService;
import clinic.service.PharmacyService;
import clinic.service.WorkProgressService;

import java.nio.file.Path;

public class AppContext {
    private final AuthService authService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final ConsultationService consultationService;
    private final PharmacyService pharmacyService;
    private final ExpertSessionService expertSessionService;
    private final CaseRecordService caseRecordService;
    private final WorkProgressService workProgressService;
    private final CalendarEventService calendarEventService;
    private final MeetingMinuteService meetingMinuteService;
    private final ExpertAdviceService expertAdviceService;
    private final InsightService insightService;

    public AppContext(Path dataDirectory) {
        Path users = dataDirectory.resolve("users.csv");
        Path patients = dataDirectory.resolve("patients.csv");
        Path doctors = dataDirectory.resolve("doctors.csv");
        Path appointments = dataDirectory.resolve("appointments.csv");
        Path consultations = dataDirectory.resolve("consultations.csv");
        Path medicines = dataDirectory.resolve("medicines.csv");
        Path prescriptions = dataDirectory.resolve("prescriptions.csv");
        Path expertSessions = dataDirectory.resolve("expert_sessions.csv");
        Path expertParticipants = dataDirectory.resolve("expert_participants.csv");
        Path caseLibrary = dataDirectory.resolve("case_library.csv");
        Path workProgress = dataDirectory.resolve("work_progress.csv");
        Path calendarEvents = dataDirectory.resolve("calendar_events.csv");
        Path meetingMinutes = dataDirectory.resolve("meeting_minutes.csv");
        Path expertAdvices = dataDirectory.resolve("expert_advices.csv");

        UserRepository userRepository = new UserRepository(users);
        PatientRepository patientRepository = new PatientRepository(patients);
        DoctorRepository doctorRepository = new DoctorRepository(doctors);
        AppointmentRepository appointmentRepository = new AppointmentRepository(appointments);
        ConsultationRepository consultationRepository = new ConsultationRepository(consultations);
        MedicineRepository medicineRepository = new MedicineRepository(medicines);
        PrescriptionRepository prescriptionRepository = new PrescriptionRepository(prescriptions);
        ExpertSessionRepository expertSessionRepository = new ExpertSessionRepository(expertSessions);
        ExpertParticipantRepository expertParticipantRepository = new ExpertParticipantRepository(expertParticipants);
        CaseRecordRepository caseRecordRepository = new CaseRecordRepository(caseLibrary);
        WorkProgressRepository workProgressRepository = new WorkProgressRepository(workProgress);
        CalendarEventRepository calendarEventRepository = new CalendarEventRepository(calendarEvents);
        MeetingMinuteRepository meetingMinuteRepository = new MeetingMinuteRepository(meetingMinutes);
        ExpertAdviceRepository expertAdviceRepository = new ExpertAdviceRepository(expertAdvices);

        this.authService = new AuthService(userRepository, patientRepository);
        this.patientService = new PatientService(patientRepository);
        this.doctorService = new DoctorService(doctorRepository);
        this.appointmentService = new AppointmentService(appointmentRepository);
        this.consultationService = new ConsultationService(consultationRepository);
        this.pharmacyService = new PharmacyService(medicineRepository, prescriptionRepository);
        this.expertSessionService = new ExpertSessionService(expertSessionRepository, expertParticipantRepository);
        this.caseRecordService = new CaseRecordService(caseRecordRepository);
        this.workProgressService = new WorkProgressService(workProgressRepository);
        this.calendarEventService = new CalendarEventService(calendarEventRepository);
        this.meetingMinuteService = new MeetingMinuteService(meetingMinuteRepository);
        this.expertAdviceService = new ExpertAdviceService(expertAdviceRepository);
        this.insightService = new InsightService(
            this.patientService,
            this.doctorService,
            this.caseRecordService,
            this.consultationService,
            this.workProgressService,
            this.expertAdviceService
        );
    }

    public AuthService getAuthService() {
        return authService;
    }

    public PatientService getPatientService() {
        return patientService;
    }

    public DoctorService getDoctorService() {
        return doctorService;
    }

    public AppointmentService getAppointmentService() {
        return appointmentService;
    }

    public ConsultationService getConsultationService() {
        return consultationService;
    }

    public PharmacyService getPharmacyService() {
        return pharmacyService;
    }

    public ExpertSessionService getExpertSessionService() {
        return expertSessionService;
    }

    public CaseRecordService getCaseRecordService() {
        return caseRecordService;
    }

    public WorkProgressService getWorkProgressService() {
        return workProgressService;
    }

    public CalendarEventService getCalendarEventService() {
        return calendarEventService;
    }

    public MeetingMinuteService getMeetingMinuteService() {
        return meetingMinuteService;
    }

    public ExpertAdviceService getExpertAdviceService() {
        return expertAdviceService;
    }

    public InsightService getInsightService() {
        return insightService;
    }
}
