package clinic.service;

import clinic.model.CaseRecord;
import clinic.model.Consultation;
import clinic.model.Doctor;
import clinic.model.ExpertAdvice;
import clinic.model.Patient;
import clinic.model.WorkProgress;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class InsightService {
    private static final int MAX_CASE_ITEMS = 3;
    private static final int MAX_CONSULTATION_ITEMS = 3;
    private static final int MAX_ADVICE_ITEMS = 3;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final CaseRecordService caseRecordService;
    private final ConsultationService consultationService;
    private final WorkProgressService workProgressService;
    private final ExpertAdviceService expertAdviceService;

    public InsightService(
        PatientService patientService,
        DoctorService doctorService,
        CaseRecordService caseRecordService,
        ConsultationService consultationService,
        WorkProgressService workProgressService,
        ExpertAdviceService expertAdviceService
    ) {
        this.patientService = Objects.requireNonNull(patientService);
        this.doctorService = Objects.requireNonNull(doctorService);
        this.caseRecordService = Objects.requireNonNull(caseRecordService);
        this.consultationService = Objects.requireNonNull(consultationService);
        this.workProgressService = Objects.requireNonNull(workProgressService);
        this.expertAdviceService = Objects.requireNonNull(expertAdviceService);
    }

    public String buildPatientInsight(String patientId) throws IOException {
        Patient patient = locatePatient(patientId);
        List<CaseRecord> caseRecords = caseRecordService.listByPatient(patientId);
        List<Consultation> consultations = consultationService.listByPatient(patientId);
        List<WorkProgress> progressList = workProgressService.listByPatient(patientId);
        List<ExpertAdvice> advices = expertAdviceService.listByPatient(patientId);

        StringBuilder builder = new StringBuilder();
        builder.append("患者概况\n------------------------------\n");
        builder.append("姓名: ").append(patient.getName());
        if (patient.getGender() != null && !patient.getGender().isBlank()) {
            builder.append(" | 性别: ").append(patient.getGender());
        }
        if (patient.getBirthday() != null) {
            builder.append(" | 年龄: ").append(calculateAge(patient.getBirthday())).append("岁");
        }
        if (patient.getNotes() != null && !patient.getNotes().isBlank()) {
            builder.append("\n备注: ").append(patient.getNotes());
        }
        builder.append("\n\n病例亮点\n------------------------------\n");
        if (caseRecords.isEmpty()) {
            builder.append("暂无历史病例记录\n");
        } else {
            caseRecords.stream()
                .limit(MAX_CASE_ITEMS)
                .forEach(record -> builder.append("- ")
                    .append(record.getTitle())
                    .append("｜标签: ")
                    .append(record.getTags() == null || record.getTags().isBlank() ? "未标注" : record.getTags())
                    .append("\n  摘要: ")
                    .append(snippet(record.getSummary()))
                    .append("\n"));
            if (caseRecords.size() > MAX_CASE_ITEMS) {
                builder.append("... 其余 ").append(caseRecords.size() - MAX_CASE_ITEMS).append(" 条病例可在病例库中查看\n");
            }
        }

        builder.append("\n近期问诊\n------------------------------\n");
        if (consultations.isEmpty()) {
            builder.append("暂无问诊记录\n");
        } else {
            consultations.stream()
                .sorted(Comparator.comparing(Consultation::getCreatedAt).reversed())
                .limit(MAX_CONSULTATION_ITEMS)
                .forEach(consultation -> builder.append("- ")
                    .append(DATE_TIME_FORMATTER.format(consultation.getCreatedAt()))
                    .append(" 由医生 ")
                    .append(consultation.getDoctorId())
                    .append(" 提供问诊\n  要点: ")
                    .append(snippet(consultation.getSummary()))
                    .append("\n"));
            if (consultations.size() > MAX_CONSULTATION_ITEMS) {
                builder.append("... 其余 ").append(consultations.size() - MAX_CONSULTATION_ITEMS).append(" 条问诊可在问诊记录中查看\n");
            }
        }

        builder.append("\n跟进进度\n------------------------------\n");
        if (progressList.isEmpty()) {
            builder.append("暂无工作进度记录\n");
        } else {
            Map<String, Long> statusCounts = progressList.stream()
                .collect(Collectors.groupingBy(progress -> normalize(progress.getStatus()), Collectors.counting()));
            StringJoiner statusJoiner = new StringJoiner("，");
            statusCounts.forEach((status, count) -> statusJoiner.add(status + ": " + count + " 项"));
            builder.append("状态概览: ").append(statusJoiner).append("\n");
            Optional<WorkProgress> latest = progressList.stream()
                .filter(item -> item.getLastUpdated() != null)
                .max(Comparator.comparing(WorkProgress::getLastUpdated));
            latest.ifPresent(item -> builder.append("最近更新: ")
                .append(item.getLastUpdated())
                .append(" ｜ ")
                .append(item.getDescription())
                .append(" (状态: ")
                .append(item.getStatus())
                .append(")\n"));
            builder.append("跟进建议: ").append(progressFollowUpSuggestion(statusCounts, latest)).append("\n");
        }

        builder.append("\n专家建议\n------------------------------\n");
        if (advices.isEmpty()) {
            builder.append("暂无专家建议\n");
        } else {
            advices.stream()
                .sorted(Comparator.comparing(ExpertAdvice::getAdviceDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(MAX_ADVICE_ITEMS)
                .forEach(advice -> builder.append("- ")
                    .append(advice.getAdviceDate())
                    .append(" ｜ ")
                    .append(snippet(advice.getAdviceSummary()))
                    .append("\n  随访: ")
                    .append(snippet(advice.getFollowUpPlan()))
                    .append("\n"));
            if (advices.size() > MAX_ADVICE_ITEMS) {
                builder.append("... 其余 ").append(advices.size() - MAX_ADVICE_ITEMS).append(" 条建议可在专家建议模块查看\n");
            }
        }

        return builder.toString();
    }

    public String buildDoctorWeeklySummary(String doctorId, LocalDate reference) throws IOException {
        Doctor doctor = locateDoctor(doctorId);
        LocalDate end = reference == null ? LocalDate.now() : reference;
        LocalDate start = end.minusDays(6);

        List<WorkProgress> progressItems = workProgressService.listAll().stream()
            .filter(item -> doctorId.equals(item.getOwnerDoctorId()))
            .filter(item -> item.getLastUpdated() != null)
            .filter(item -> !item.getLastUpdated().isBefore(start) && !item.getLastUpdated().isAfter(end))
            .collect(Collectors.toList());

        List<ExpertAdvice> adviceItems = expertAdviceService.listAll().stream()
            .filter(item -> doctorId.equals(item.getDoctorId()))
            .filter(item -> item.getAdviceDate() != null)
            .filter(item -> !item.getAdviceDate().isBefore(start) && !item.getAdviceDate().isAfter(end))
            .collect(Collectors.toList());

        List<Consultation> consultationItems = consultationService.listConsultations().stream()
            .filter(item -> doctorId.equals(item.getDoctorId()))
            .filter(item -> isWithin(item.getCreatedAt(), start, end))
            .collect(Collectors.toList());

        StringBuilder builder = new StringBuilder();
        builder.append("医生周报\n------------------------------\n");
        builder.append("医生: ").append(doctor.getName());
        if (doctor.getDepartment() != null && !doctor.getDepartment().isBlank()) {
            builder.append("｜科室: ").append(doctor.getDepartment());
        }
        builder.append("\n统计范围: ").append(start).append(" 至 ").append(end).append("\n\n");

        if (progressItems.isEmpty() && adviceItems.isEmpty() && consultationItems.isEmpty()) {
            builder.append("本周暂无进度、问诊或专家建议记录。请确认数据是否已录入。\n");
            return builder.toString();
        }

        if (!progressItems.isEmpty()) {
            Map<String, Long> statusCounts = progressItems.stream()
                .collect(Collectors.groupingBy(item -> normalize(item.getStatus()), Collectors.counting()));
            builder.append("工作进度统计\n------------------------------\n");
            builder.append("累计处理患者: ").append(countDistinctPatients(progressItems)).append(" 名\n");
            statusCounts.forEach((status, count) -> builder.append("- ").append(status).append(": ").append(count).append(" 项\n"));
            progressItems.stream()
                .sorted(Comparator.comparing(WorkProgress::getLastUpdated).reversed())
                .limit(3)
                .forEach(item -> builder.append("  · ")
                    .append(item.getLastUpdated())
                    .append(" ｜ 患者 ")
                    .append(item.getPatientId())
                    .append(" ｜ ")
                    .append(item.getDescription())
                    .append(" (状态: ")
                    .append(item.getStatus())
                    .append(")\n"));
            builder.append("建议: ").append(progressFollowUpSuggestion(statusCounts, progressItems.stream()
                .filter(p -> p.getLastUpdated() != null)
                .max(Comparator.comparing(WorkProgress::getLastUpdated)))).append("\n\n");
        }

        if (!consultationItems.isEmpty()) {
            builder.append("问诊亮点\n------------------------------\n");
            builder.append("累计问诊: ").append(consultationItems.size()).append(" 场\n");
            consultationItems.stream()
                .sorted(Comparator.comparing(Consultation::getCreatedAt).reversed())
                .limit(3)
                .forEach(item -> builder.append("- ")
                    .append(DATE_TIME_FORMATTER.format(item.getCreatedAt()))
                    .append(" ｜ 患者 ")
                    .append(item.getPatientId())
                    .append(" ｜ ")
                    .append(snippet(item.getSummary()))
                    .append("\n"));
            builder.append("\n");
        }

        if (!adviceItems.isEmpty()) {
            builder.append("专家建议/随访\n------------------------------\n");
            builder.append("本周参与建议: ").append(adviceItems.size()).append(" 条\n");
            adviceItems.stream()
                .sorted(Comparator.comparing(ExpertAdvice::getAdviceDate).reversed())
                .limit(3)
                .forEach(item -> builder.append("- ")
                    .append(item.getAdviceDate())
                    .append(" ｜ 患者 ")
                    .append(item.getPatientId())
                    .append(" ｜ ")
                    .append(snippet(item.getAdviceSummary()))
                    .append("\n"));
        }

        return builder.toString();
    }

    private Patient locatePatient(String patientId) throws IOException {
        return patientService.listPatients().stream()
            .filter(patient -> patient.getId().equals(patientId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到指定患者"));
    }

    private Doctor locateDoctor(String doctorId) throws IOException {
        return doctorService.listDoctors().stream()
            .filter(doctor -> doctor.getId().equals(doctorId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到指定医生"));
    }

    private int calculateAge(LocalDate birthday) {
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    private String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "暂无内容";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 48 ? trimmed : trimmed.substring(0, 45) + "...";
    }

    private String normalize(String status) {
        if (status == null || status.isBlank()) {
            return "未标明";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private long countDistinctPatients(List<WorkProgress> progresses) {
        return progresses.stream()
            .map(WorkProgress::getPatientId)
            .collect(Collectors.toSet())
            .size();
    }

    private boolean isWithin(LocalDateTime time, LocalDate start, LocalDate end) {
        LocalDate date = time.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private String progressFollowUpSuggestion(Map<String, Long> statusCounts, Optional<WorkProgress> latest) {
        long pending = statusCounts.getOrDefault("PENDING", 0L);
        long inProgress = statusCounts.getOrDefault("IN_PROGRESS", 0L);
        long done = statusCounts.getOrDefault("DONE", 0L);

        if (pending + inProgress >= 3) {
            return "存在多项待处理任务，建议安排复诊或协调团队加快推进";
        }
        if (pending > 0) {
            return "存在未开始的任务，建议尽快安排执行";
        }
        if (inProgress > 0 && latest.isPresent()) {
            return "已有任务在进行中，关注进展并适时复查";
        }
        if (done > 0) {
            return "当前任务均已完成，可安排巩固随访";
        }
        return "暂无明确建议";
    }
}
