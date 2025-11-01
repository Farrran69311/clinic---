package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Doctor;
import clinic.model.ExpertParticipant;
import clinic.model.ExpertSession;
import clinic.model.MeetingMinute;
import clinic.model.Patient;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpertSessionManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public ExpertSessionManagementPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "主题", "主持医生", "时间", "状态", "会议链接", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("专家会诊室"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("新增");
        JButton editButton = new JButton("编辑");
        JButton deleteButton = new JButton("删除");
        JButton participantsButton = new JButton("参与人员");
        JButton minutesButton = new JButton("查看纪要");
        JButton addMinuteButton = new JButton("新增纪要");
        controls.add(addButton);
        controls.add(editButton);
        controls.add(deleteButton);
        controls.add(participantsButton);
        controls.add(minutesButton);
        controls.add(addMinuteButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> createSession());
        editButton.addActionListener(e -> editSession());
        deleteButton.addActionListener(e -> deleteSession());
        participantsButton.addActionListener(e -> manageParticipants());
        minutesButton.addActionListener(e -> viewMinutes());
        addMinuteButton.addActionListener(e -> addMinute());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> doctorNames = buildDoctorNames();
            for (ExpertSession session : context.getExpertSessionService().listSessions()) {
                model.addRow(new Object[]{
                    session.getId(),
                    session.getTitle(),
                    doctorNames.getOrDefault(session.getHostDoctorId(), session.getHostDoctorId()),
                    session.getScheduledAt(),
                    session.getStatus(),
                    session.getMeetingUrl(),
                    session.getNotes()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载会诊失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSession() {
        Map<String, String> doctorNames;
        try {
            doctorNames = buildDoctorNames();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载医生信息失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SessionForm form = new SessionForm(doctorNames);
        if (form.showDialog(this, "新增会诊")) {
            try {
                context.getExpertSessionService().createSession(
                    form.titleField.getText().trim(),
                    form.resolveDoctorId(),
                    form.parseDateTime(),
                    form.statusField.getText().trim().isEmpty() ? "SCHEDULED" : form.statusField.getText().trim(),
                    form.meetingField.getText().trim(),
                    form.notesField.getText().trim()
                );
                refreshData();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSession() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择会诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        Optional<ExpertSession> optional;
        try {
            optional = findSessionById(id);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载会诊失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (optional.isEmpty()) {
            JOptionPane.showMessageDialog(this, "未找到会诊记录", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ExpertSession session = optional.get();
        Map<String, String> doctorNames;
        try {
            doctorNames = buildDoctorNames();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载医生信息失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SessionForm form = new SessionForm(doctorNames);
        form.titleField.setText(session.getTitle());
        form.setDoctor(session.getHostDoctorId());
        form.datetimeField.setText(session.getScheduledAt() == null ? "" : session.getScheduledAt().toString());
        form.statusField.setText(session.getStatus());
        form.meetingField.setText(session.getMeetingUrl() == null ? "" : session.getMeetingUrl());
        form.notesField.setText(session.getNotes());
        if (form.showDialog(this, "编辑会诊")) {
            try {
                ExpertSession updated = new ExpertSession(
                    session.getId(),
                    form.titleField.getText().trim(),
                    form.resolveDoctorId(),
                    form.parseDateTime(),
                    form.statusField.getText().trim().isEmpty() ? session.getStatus() : form.statusField.getText().trim(),
                    form.meetingField.getText().trim(),
                    form.notesField.getText().trim()
                );
                context.getExpertSessionService().updateSession(updated);
                refreshData();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSession() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的会诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        if (JOptionPane.showConfirmDialog(this, "确认删除该会诊?", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                context.getExpertSessionService().deleteSession(id);
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void manageParticipants() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择会诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String sessionId = model.getValueAt(row, 0).toString();
        ParticipantsDialog dialog = new ParticipantsDialog(sessionId);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void viewMinutes() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择会诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String sessionId = model.getValueAt(row, 0).toString();
        try {
            List<MeetingMinute> minutes = context.getMeetingMinuteService().listBySession(sessionId);
            if (minutes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "该会诊暂无会议纪要", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (MeetingMinute minute : minutes) {
                builder.append("时间: ").append(formatDateTime(minute.getRecordedAt())).append('\n')
                    .append("撰写人: ").append(resolveDoctorName(minute.getAuthorDoctorId())).append('\n')
                    .append("概要: ").append(blankSafe(minute.getSummary())).append('\n')
                    .append("行动项: ").append(blankSafe(minute.getActionItems())).append("\n\n");
            }
            JOptionPane.showMessageDialog(this, builder.toString(), "会议纪要", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载纪要失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addMinute() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择会诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String sessionId = model.getValueAt(row, 0).toString();
        try {
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            if (doctors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无医生信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<String> doctorCombo = new JComboBox<>(doctors.stream().map(Doctor::getName).toArray(String[]::new));
            JTextField summaryField = new JTextField();
            JTextField actionField = new JTextField();
            Object[] message = {
                "纪要医生", doctorCombo,
                "概要", summaryField,
                "行动项", actionField
            };
            if (JOptionPane.showConfirmDialog(this, message, "新增纪要", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                String doctorId = doctors.get(doctorCombo.getSelectedIndex()).getId();
                context.getMeetingMinuteService().createMinute(sessionId, doctorId, summaryField.getText().trim(), actionField.getText().trim());
                JOptionPane.showMessageDialog(this, "已保存会议纪要", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Optional<ExpertSession> findSessionById(String id) throws IOException {
        return context.getExpertSessionService().listSessions().stream()
            .filter(s -> s.getId().equals(id))
            .findFirst();
    }

    private Map<String, String> buildDoctorNames() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Doctor doctor : context.getDoctorService().listDoctors()) {
            map.put(doctor.getId(), doctor.getName());
        }
        return map;
    }

    private String resolveDoctorName(String doctorId) throws IOException {
        if (doctorId == null || doctorId.isEmpty()) {
            return "-";
        }
        return buildDoctorNames().getOrDefault(doctorId, doctorId);
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "-" : time.toString().replace('T', ' ');
    }

    private String blankSafe(String value) {
        return value == null || value.isBlank() ? "暂无" : value;
    }

    private class ParticipantsDialog extends JDialog {
        private final DefaultTableModel participantModel;
        private final JTable participantTable;
        private final String sessionId;

        ParticipantsDialog(String sessionId) {
            super(JOptionPane.getFrameForComponent(ExpertSessionManagementPanel.this), "会诊参与人员", true);
            this.sessionId = sessionId;
            setSize(540, 320);
            setLayout(new BorderLayout(10, 10));

            participantModel = new DefaultTableModel(new String[]{"编号", "姓名", "身份", "角色"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            participantTable = new JTable(participantModel);
            add(new JScrollPane(participantTable), BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addButton = new JButton("新增");
            JButton removeButton = new JButton("移除");
            JButton closeButton = new JButton("关闭");
            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            buttonPanel.add(closeButton);
            add(buttonPanel, BorderLayout.SOUTH);

            addButton.addActionListener(e -> addParticipant());
            removeButton.addActionListener(e -> removeParticipant());
            closeButton.addActionListener(e -> dispose());

            refreshParticipants();
        }

        private void refreshParticipants() {
            participantModel.setRowCount(0);
            try {
                Map<String, String> doctorNames = buildDoctorNames();
                Map<String, String> patientNames = new HashMap<>();
                for (Patient patient : context.getPatientService().listPatients()) {
                    patientNames.put(patient.getId(), patient.getName());
                }
                for (ExpertParticipant participant : context.getExpertSessionService().listParticipants(sessionId)) {
                    String id = participant.getParticipantId();
                    String role = doctorNames.containsKey(id) ? "医生" : patientNames.containsKey(id) ? "患者" : "其他";
                    String name = doctorNames.getOrDefault(id, patientNames.getOrDefault(id, id));
                    participantModel.addRow(new Object[]{id, name, role, participant.getParticipantRole()});
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "加载参与者失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void addParticipant() {
            try {
                List<Doctor> doctors = context.getDoctorService().listDoctors();
                List<Patient> patients = context.getPatientService().listPatients();
                if (doctors.isEmpty() && patients.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "暂无可添加人员", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                JComboBox<String> typeCombo = new JComboBox<>(new String[]{"医生", "患者"});
                JComboBox<String> personCombo = new JComboBox<>();
                populatePersonCombo(typeCombo, personCombo, doctors, patients);
                typeCombo.addActionListener(e -> populatePersonCombo(typeCombo, personCombo, doctors, patients));
                JTextField roleField = new JTextField();
                JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
                form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                form.add(new JLabel("身份"));
                form.add(typeCombo);
                form.add(new JLabel("人员"));
                form.add(personCombo);
                form.add(new JLabel("会诊角色"));
                form.add(roleField);
                if (JOptionPane.showConfirmDialog(this, form, "新增参与者", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    if (personCombo.getSelectedItem() == null) {
                        JOptionPane.showMessageDialog(this, "请选择人员", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String id;
                    if ("医生".equals(typeCombo.getSelectedItem())) {
                        id = doctors.get(personCombo.getSelectedIndex()).getId();
                    } else {
                        id = patients.get(personCombo.getSelectedIndex()).getId();
                    }
                    context.getExpertSessionService().addParticipant(sessionId, id, roleField.getText().trim());
                    refreshParticipants();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void removeParticipant() {
            int row = participantTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "请选择要移除的人员", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String participantId = participantModel.getValueAt(row, 0).toString();
            try {
                context.getExpertSessionService().removeParticipant(sessionId, participantId);
                refreshParticipants();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "移除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void populatePersonCombo(JComboBox<String> typeCombo, JComboBox<String> personCombo, List<Doctor> doctors, List<Patient> patients) {
            personCombo.removeAllItems();
            if ("医生".equals(typeCombo.getSelectedItem())) {
                for (Doctor doctor : doctors) {
                    personCombo.addItem(doctor.getName());
                }
            } else {
                for (Patient patient : patients) {
                    personCombo.addItem(patient.getName());
                }
            }
        }
    }

    private static class SessionForm {
        final JTextField titleField = new JTextField();
        final JComboBox<String> doctorCombo;
        final Map<String, String> doctorIdByName;
        final JTextField datetimeField = new JTextField("2025-01-01T09:00");
        final JTextField statusField = new JTextField("SCHEDULED");
        final JTextField meetingField = new JTextField();
        final JTextField notesField = new JTextField();

        SessionForm(Map<String, String> doctorNames) {
            doctorIdByName = new HashMap<>();
            doctorCombo = new JComboBox<>(doctorNames.values().toArray(new String[0]));
            for (Map.Entry<String, String> entry : doctorNames.entrySet()) {
                doctorIdByName.put(entry.getValue(), entry.getKey());
            }
        }

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "主题", titleField,
                "主持医生", doctorCombo,
                "开始时间 (YYYY-MM-DDTHH:MM)", datetimeField,
                "状态", statusField,
                "会议链接", meetingField,
                "备注", notesField
            };
            return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        String resolveDoctorId() {
            if (doctorCombo.getSelectedItem() == null) {
                throw new IllegalArgumentException("请选择主持医生");
            }
            String name = doctorCombo.getSelectedItem().toString();
            return doctorIdByName.getOrDefault(name, name);
        }

        void setDoctor(String doctorId) {
            for (Map.Entry<String, String> entry : doctorIdByName.entrySet()) {
                if (entry.getValue().equals(doctorId)) {
                    doctorCombo.setSelectedItem(entry.getKey());
                    return;
                }
            }
        }

        LocalDateTime parseDateTime() {
            String value = datetimeField.getText().trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("时间格式应为YYYY-MM-DDTHH:MM");
            }
        }
    }
}
