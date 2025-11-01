package clinic.ui;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.CalendarEvent;
import clinic.model.Doctor;
import clinic.model.ExpertAdvice;
import clinic.model.Patient;
import clinic.model.Role;
import clinic.model.User;
import clinic.model.WorkProgress;
import clinic.ui.common.UIUtils;
import clinic.util.DoctorMatcher;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class WelcomePanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA);
    private static final DateTimeFormatter TIME_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA);
    private static final DateTimeFormatter SCHEDULE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA);
    private static final DateTimeFormatter FULL_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.CHINA);

    private final AppContext context;
    private final User user;
    private final JLabel greetingLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JTextArea summaryArea = new JTextArea();
    private final JPanel scheduleContainer = new JPanel();

    public WelcomePanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        buildUI();
        refreshData();
    }

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);

        greetingLabel.setFont(greetingLabel.getFont().deriveFont(Font.BOLD, 20f));
        dateLabel.setFont(dateLabel.getFont().deriveFont(14f));
        dateLabel.setForeground(new Color(96, 96, 96));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(greetingLabel, BorderLayout.WEST);
        header.add(dateLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

    JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
    cards.add(buildSummaryCard());
    cards.add(buildScheduleCard());
        add(cards, BorderLayout.CENTER);
    }

    private JPanel buildSummaryCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("今日提醒"));

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setOpaque(false);
        summaryArea.setFont(summaryArea.getFont().deriveFont(14f));

        JScrollPane scrollPane = new JScrollPane(summaryArea);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildScheduleCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("日程安排"));

        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(scheduleContainer);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(DATE_DISPLAY_FORMAT.format(now) + " ｜ 当前时间 " + TIME_DISPLAY_FORMAT.format(now));
        try {
            if (user.getRole() == Role.PATIENT) {
                updateForPatient(now);
            } else if (user.getRole() == Role.DOCTOR) {
                updateForDoctor(now);
            } else {
                updateForGeneral(now);
            }
        } catch (IOException ex) {
            summaryArea.setText("加载欢迎信息失败: " + ex.getMessage());
            rebuildSchedule(Collections.emptyList(), "无法获取日程，请稍后刷新。");
        }
    }

    private void updateForPatient(LocalDateTime now) throws IOException {
        Map<String, String> doctorNames = buildDoctorNameMap();
        String displayName = context.getPatientService().listPatients().stream()
            .filter(patient -> patient.getId().equals(user.getId()))
            .map(Patient::getName)
            .findFirst()
            .orElse(user.getUsername());
        greetingLabel.setText("欢迎 " + displayName + " 使用在线诊所管理系统");

        List<Appointment> upcomingAppointments = context.getAppointmentService().listAppointments().stream()
            .filter(appointment -> appointment.getPatientId().equals(user.getId()))
            .filter(appointment -> !appointment.getDateTime().isBefore(now))
            .sorted(Comparator.comparing(Appointment::getDateTime))
            .collect(Collectors.toList());

        List<CalendarEvent> upcomingEvents = context.getCalendarEventService().listAll().stream()
            .filter(event -> user.getId().equals(event.getRelatedPatientId()))
            .filter(event -> event.getStart() != null && !event.getStart().isBefore(now))
            .sorted(Comparator.comparing(CalendarEvent::getStart))
            .collect(Collectors.toList());

        List<ScheduleEntry> scheduleEntries = new ArrayList<>();
        for (Appointment appointment : upcomingAppointments) {
            scheduleEntries.add(new ScheduleEntry(
                appointment.getDateTime(),
                describeAppointmentForPatient(appointment, doctorNames)
            ));
        }
        for (CalendarEvent event : upcomingEvents) {
            scheduleEntries.add(new ScheduleEntry(
                event.getStart(),
                describeEventForPatient(event)
            ));
        }
        Collections.sort(scheduleEntries, Comparator.comparing(ScheduleEntry::time));
        rebuildSchedule(limitEntries(scheduleEntries), "今天暂无安排，祝您健康！");

        List<WorkProgress> progresses = context.getWorkProgressService().listByPatient(user.getId());
        long pendingTasks = progresses.stream().filter(progress -> !isClosedStatus(progress.getStatus())).count();
        Optional<WorkProgress> latestProgress = progresses.stream()
            .filter(progress -> progress.getLastUpdated() != null)
            .max(Comparator.comparing(WorkProgress::getLastUpdated));

        List<ExpertAdvice> advices = context.getExpertAdviceService().listByPatient(user.getId());
        Optional<ExpertAdvice> latestAdvice = advices.stream()
            .filter(advice -> advice.getAdviceDate() != null)
            .max(Comparator.comparing(ExpertAdvice::getAdviceDate));

        Optional<Appointment> nextAppointment = upcomingAppointments.stream().findFirst();

        StringBuilder summary = new StringBuilder();
        summary.append("今日概览：\n");
        if (nextAppointment.isPresent()) {
            Appointment appointment = nextAppointment.get();
            summary.append("- 最近预约：")
                .append(FULL_DISPLAY_FORMAT.format(appointment.getDateTime()))
                .append(" ｜ 医生 ")
                .append(doctorNames.getOrDefault(appointment.getDoctorId(), appointment.getDoctorId()))
                .append(" ｜ 状态 ")
                .append(translateStatus(appointment.getStatus()))
                .append("\n");
        } else {
            summary.append("- 近期暂无预约安排\n");
        }
        summary.append("- 跟进任务：").append(pendingTasks).append(" 项待处理\n");
        latestProgress.ifPresent(progress -> summary.append("- 最近更新：")
            .append(progress.getLastUpdated())
            .append(" ｜ ")
            .append(progress.getDescription())
            .append("\n"));
        if (latestAdvice.isPresent()) {
            ExpertAdvice advice = latestAdvice.get();
            summary.append("- 最新专家建议：")
                .append(advice.getAdviceDate())
                .append(" ｜ ")
                .append(snippet(advice.getAdviceSummary()))
                .append("\n");
        } else {
            summary.append("- 暂无新的专家建议\n");
        }
        summaryArea.setText(summary.toString());
        summaryArea.setCaretPosition(0);
    }

    private void updateForDoctor(LocalDateTime now) throws IOException {
    Map<String, String> patientNames = buildPatientNameMap();
    Map<String, String> doctorNames = buildDoctorNameMap();
        Optional<Doctor> doctorOptional = resolveDoctor();
        String displayName = doctorOptional.map(Doctor::getName).orElse(user.getUsername());
        greetingLabel.setText("欢迎 " + displayName + " 使用在线诊所管理系统");

        List<ScheduleEntry> scheduleEntries = new ArrayList<>();
        List<Appointment> allAppointments = context.getAppointmentService().listAppointments();
        List<CalendarEvent> allEvents = context.getCalendarEventService().listAll();

        if (doctorOptional.isPresent()) {
            String doctorId = doctorOptional.get().getId();
            for (Appointment appointment : allAppointments) {
                if (doctorId.equals(appointment.getDoctorId()) && !appointment.getDateTime().isBefore(now)) {
                    scheduleEntries.add(new ScheduleEntry(
                        appointment.getDateTime(),
                        describeAppointmentForDoctor(appointment, patientNames)
                    ));
                }
            }
            for (CalendarEvent event : allEvents) {
                if (doctorId.equals(event.getOwnerDoctorId()) && event.getStart() != null && !event.getStart().isBefore(now)) {
                    scheduleEntries.add(new ScheduleEntry(
                        event.getStart(),
                        describeEventForDoctor(event, patientNames)
                    ));
                }
            }
            Collections.sort(scheduleEntries, Comparator.comparing(ScheduleEntry::time));
            rebuildSchedule(limitEntries(scheduleEntries), "暂无后续日程，请关注新的预约或排班。");

            List<WorkProgress> tasks = context.getWorkProgressService().listAll().stream()
                .filter(progress -> doctorId.equals(progress.getOwnerDoctorId()))
                .collect(Collectors.toList());
            long pendingTasks = tasks.stream().filter(progress -> !isClosedStatus(progress.getStatus())).count();
            long finishedTasks = tasks.stream().filter(progress -> isClosedStatus(progress.getStatus())).count();
            Optional<WorkProgress> latest = tasks.stream()
                .filter(progress -> progress.getLastUpdated() != null)
                .max(Comparator.comparing(WorkProgress::getLastUpdated));

            Optional<Appointment> nextAppointment = allAppointments.stream()
                .filter(appointment -> doctorId.equals(appointment.getDoctorId()))
                .filter(appointment -> !appointment.getDateTime().isBefore(now))
                .min(Comparator.comparing(Appointment::getDateTime));

            StringBuilder summary = new StringBuilder();
            summary.append("工作提醒：\n");
            summary.append("- 待处理进度：").append(pendingTasks).append(" 项\n");
            summary.append("- 已完成进度：").append(finishedTasks).append(" 项\n");
            latest.ifPresent(progress -> summary.append("- 最近更新：")
                .append(progress.getLastUpdated())
                .append(" ｜ 患者 ")
                .append(patientNames.getOrDefault(progress.getPatientId(), progress.getPatientId()))
                .append(" ｜ ")
                .append(progress.getDescription())
                .append("\n"));
            nextAppointment.ifPresent(appointment -> summary.append("- 下一场预约：")
                .append(FULL_DISPLAY_FORMAT.format(appointment.getDateTime()))
                .append(" ｜ 患者 ")
                .append(patientNames.getOrDefault(appointment.getPatientId(), appointment.getPatientId()))
                .append(" ｜ 状态 ")
                .append(translateStatus(appointment.getStatus()))
                .append("\n"));
            summaryArea.setText(summary.toString());
        } else {
            for (Appointment appointment : allAppointments) {
                if (!appointment.getDateTime().isBefore(now)) {
                    scheduleEntries.add(new ScheduleEntry(
                        appointment.getDateTime(),
                        describeAppointmentGeneral(appointment, patientNames, doctorNames)
                    ));
                }
            }
            for (CalendarEvent event : allEvents) {
                if (event.getStart() != null && !event.getStart().isBefore(now)) {
                    scheduleEntries.add(new ScheduleEntry(
                        event.getStart(),
                        describeEventGeneral(event, patientNames, doctorNames)
                    ));
                }
            }
            Collections.sort(scheduleEntries, Comparator.comparing(ScheduleEntry::time));
            rebuildSchedule(limitEntries(scheduleEntries), "暂无后续日程，请关注新的预约或排班。");
            summaryArea.setText("未找到与当前账号关联的医生档案。已为您展示全院的最新日程，请确认医生信息中的编号是否与账号匹配。");
        }
        summaryArea.setCaretPosition(0);
    }

    private void updateForGeneral(LocalDateTime now) throws IOException {
        greetingLabel.setText("欢迎 " + user.getUsername() + " 使用在线诊所管理系统");
        Map<String, String> patientNames = buildPatientNameMap();
        Map<String, String> doctorNames = buildDoctorNameMap();
        List<ScheduleEntry> scheduleEntries = new ArrayList<>();

        for (Appointment appointment : context.getAppointmentService().listAppointments()) {
            if (!appointment.getDateTime().isBefore(now)) {
                scheduleEntries.add(new ScheduleEntry(
                    appointment.getDateTime(),
                    describeAppointmentGeneral(appointment, patientNames, doctorNames)
                ));
            }
        }
        for (CalendarEvent event : context.getCalendarEventService().listAll()) {
            if (event.getStart() != null && !event.getStart().isBefore(now)) {
                scheduleEntries.add(new ScheduleEntry(
                    event.getStart(),
                    describeEventGeneral(event, patientNames, doctorNames)
                ));
            }
        }
        Collections.sort(scheduleEntries, Comparator.comparing(ScheduleEntry::time));
        rebuildSchedule(limitEntries(scheduleEntries), "暂无后续日程。");
        summaryArea.setText("这里将展示全院的预约与会议动态，您可以通过上方标签进入各个功能模块。");
        summaryArea.setCaretPosition(0);
    }

    private List<ScheduleEntry> limitEntries(List<ScheduleEntry> entries) {
        return entries.stream()
            .limit(6)
            .collect(Collectors.toList());
    }

    private void rebuildSchedule(List<ScheduleEntry> entries, String emptyMessage) {
        scheduleContainer.removeAll();
        if (entries.isEmpty()) {
            JLabel label = new JLabel(emptyMessage);
            label.setBorder(new EmptyBorder(6, 4, 6, 4));
            label.setForeground(new Color(120, 120, 120));
            scheduleContainer.add(label);
        } else {
            for (ScheduleEntry entry : entries) {
                JLabel label = new JLabel(SCHEDULE_DISPLAY_FORMAT.format(entry.time()) + " ｜ " + entry.description());
                label.setBorder(new EmptyBorder(6, 4, 6, 4));
                scheduleContainer.add(label);
            }
        }
        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }

    private Map<String, String> buildDoctorNameMap() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Doctor doctor : context.getDoctorService().listDoctors()) {
            map.put(doctor.getId(), doctor.getName());
        }
        return map;
    }

    private Map<String, String> buildPatientNameMap() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Patient patient : context.getPatientService().listPatients()) {
            map.put(patient.getId(), patient.getName());
        }
        return map;
    }

    private Optional<Doctor> resolveDoctor() throws IOException {
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        for (Doctor doctor : doctors) {
            if (doctor.getId().equals(user.getId())) {
                return Optional.of(doctor);
            }
        }
        for (Doctor doctor : doctors) {
            if (doctor.getName().equalsIgnoreCase(user.getUsername())) {
                return Optional.of(doctor);
            }
        }
        String normalizedUsername = DoctorMatcher.normalizeName(user.getUsername());
        if (!normalizedUsername.isBlank()) {
            for (Doctor doctor : doctors) {
                if (DoctorMatcher.normalizeName(doctor.getName()).equalsIgnoreCase(normalizedUsername)) {
                    return Optional.of(doctor);
                }
            }
        }
        return Optional.empty();
    }

    private String describeAppointmentForPatient(Appointment appointment, Map<String, String> doctorNames) {
        return "预约｜医生 " + doctorNames.getOrDefault(appointment.getDoctorId(), appointment.getDoctorId()) + " ｜ 状态 " + translateStatus(appointment.getStatus());
    }

    private String describeAppointmentForDoctor(Appointment appointment, Map<String, String> patientNames) {
        return "预约｜患者 " + patientNames.getOrDefault(appointment.getPatientId(), appointment.getPatientId()) + " ｜ 状态 " + translateStatus(appointment.getStatus());
    }

    private String describeAppointmentGeneral(Appointment appointment, Map<String, String> patientNames, Map<String, String> doctorNames) {
        return "预约｜医生 " + doctorNames.getOrDefault(appointment.getDoctorId(), appointment.getDoctorId()) + " ｜ 患者 " + patientNames.getOrDefault(appointment.getPatientId(), appointment.getPatientId()) + " ｜ 状态 " + translateStatus(appointment.getStatus());
    }

    private String describeEventForPatient(CalendarEvent event) {
        StringBuilder builder = new StringBuilder("日程｜");
        builder.append(event.getTitle());
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            builder.append(" ｜ ").append(event.getLocation());
        }
        return builder.toString();
    }

    private String describeEventForDoctor(CalendarEvent event, Map<String, String> patientNames) {
        StringBuilder builder = new StringBuilder("日程｜");
        builder.append(event.getTitle());
        if (event.getRelatedPatientId() != null && !event.getRelatedPatientId().isBlank()) {
            builder.append(" ｜ 患者 ").append(patientNames.getOrDefault(event.getRelatedPatientId(), event.getRelatedPatientId()));
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            builder.append(" ｜ ").append(event.getLocation());
        }
        return builder.toString();
    }

    private String describeEventGeneral(CalendarEvent event, Map<String, String> patientNames, Map<String, String> doctorNames) {
        StringBuilder builder = new StringBuilder("日程｜");
        builder.append(event.getTitle());
        if (event.getOwnerDoctorId() != null && !event.getOwnerDoctorId().isBlank()) {
            builder.append(" ｜ 医生 ").append(doctorNames.getOrDefault(event.getOwnerDoctorId(), event.getOwnerDoctorId()));
        }
        if (event.getRelatedPatientId() != null && !event.getRelatedPatientId().isBlank()) {
            builder.append(" ｜ 患者 ").append(patientNames.getOrDefault(event.getRelatedPatientId(), event.getRelatedPatientId()));
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            builder.append(" ｜ ").append(event.getLocation());
        }
        return builder.toString();
    }

    private boolean isClosedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "DONE".equals(normalized) || "COMPLETED".equals(normalized) || "RESOLVED".equals(normalized);
    }

    private String translateStatus(String status) {
        if (status == null || status.isBlank()) {
            return "未标记";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "PENDING":
                return "待确认";
            case "CONFIRMED":
                return "已确认";
            case "COMPLETED":
                return "已完成";
            case "CANCELLED":
                return "已取消";
            default:
                return status;
        }
    }

    private String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "暂无内容";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 36 ? trimmed : trimmed.substring(0, 33) + "...";
    }

    private static class ScheduleEntry {
        private final LocalDateTime time;
        private final String description;

        ScheduleEntry(LocalDateTime time, String description) {
            this.time = time;
            this.description = description;
        }

        LocalDateTime time() {
            return time;
        }

        String description() {
            return description;
        }
    }
}
