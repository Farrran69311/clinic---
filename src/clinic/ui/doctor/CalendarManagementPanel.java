package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.CalendarEvent;
import clinic.model.Doctor;
import clinic.model.Patient;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;
import clinic.ui.common.UIUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CalendarManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public CalendarManagementPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "标题", "开始时间", "结束时间", "患者", "责任医生", "地点", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("工作日程"));
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
        controls.add(addButton);
        controls.add(editButton);
        controls.add(deleteButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> createEvent());
        editButton.addActionListener(e -> editEvent());
        deleteButton.addActionListener(e -> deleteEvent());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> patientNames = buildPatientNames();
            Map<String, String> doctorNames = buildDoctorNames();
            for (CalendarEvent event : context.getCalendarEventService().listAll()) {
                model.addRow(new Object[]{
                    event.getId(),
                    event.getTitle(),
                    event.getStart(),
                    event.getEnd(),
                    patientNames.getOrDefault(event.getRelatedPatientId(), event.getRelatedPatientId()),
                    doctorNames.getOrDefault(event.getOwnerDoctorId(), event.getOwnerDoctorId()),
                    event.getLocation(),
                    event.getNotes()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载日程失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createEvent() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            EventForm form = new EventForm(patients, doctors);
            if (form.showDialog(this, "新增日程")) {
                context.getCalendarEventService().createEvent(
                    form.titleField.getText().trim(),
                    form.parseStart(),
                    form.parseEnd(),
                    form.resolvePatientId(),
                    form.resolveDoctorId(),
                    form.locationField.getText().trim(),
                    form.notesField.getText().trim()
                );
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editEvent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择日程", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        try {
            Optional<CalendarEvent> optional = context.getCalendarEventService().listAll().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
            if (optional.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到日程", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            EventForm form = new EventForm(patients, doctors);
            CalendarEvent event = optional.get();
            form.titleField.setText(event.getTitle());
            form.startField.setText(event.getStart() == null ? "" : event.getStart().toString());
            form.endField.setText(event.getEnd() == null ? "" : event.getEnd().toString());
            form.locationField.setText(event.getLocation() == null ? "" : event.getLocation());
            form.notesField.setText(event.getNotes());
            form.setDoctor(event.getOwnerDoctorId());
            form.setPatient(event.getRelatedPatientId());
            if (form.showDialog(this, "编辑日程")) {
                CalendarEvent updated = new CalendarEvent(
                    event.getId(),
                    form.titleField.getText().trim(),
                    form.parseStart(),
                    form.parseEnd(),
                    form.resolvePatientId(),
                    form.resolveDoctorId(),
                    form.locationField.getText().trim(),
                    form.notesField.getText().trim()
                );
                context.getCalendarEventService().updateEvent(updated);
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteEvent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的日程", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确认删除该日程?", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                context.getCalendarEventService().deleteEvent(model.getValueAt(row, 0).toString());
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Map<String, String> buildPatientNames() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Patient patient : context.getPatientService().listPatients()) {
            map.put(patient.getId(), patient.getName());
        }
        return map;
    }

    private Map<String, String> buildDoctorNames() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Doctor doctor : context.getDoctorService().listDoctors()) {
            map.put(doctor.getId(), doctor.getName());
        }
        return map;
    }

    private static class EventForm {
        final List<Patient> patients;
        final List<Doctor> doctors;
        final JComboBox<String> patientCombo;
        final JComboBox<String> doctorCombo;
        final JTextField titleField = new JTextField();
        final JTextField startField = new JTextField("2025-01-01T09:00");
        final JTextField endField = new JTextField("2025-01-01T10:00");
        final JTextField locationField = new JTextField();
        final JTextField notesField = new JTextField();

        EventForm(List<Patient> patients, List<Doctor> doctors) {
            this.patients = patients;
            this.doctors = doctors;
            patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
            doctorCombo = new JComboBox<>(doctors.stream().map(Doctor::getName).toArray(String[]::new));
        }

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "标题", titleField,
                "开始时间 (YYYY-MM-DDTHH:MM)", startField,
                "结束时间", endField,
                "相关患者", patientCombo,
                "责任医生", doctorCombo,
                "地点", locationField,
                "备注", notesField
            };
            return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        LocalDateTime parseStart() {
            return parseDateTime(startField.getText().trim(), "开始时间格式应为YYYY-MM-DDTHH:MM");
        }

        LocalDateTime parseEnd() {
            return parseDateTime(endField.getText().trim(), "结束时间格式应为YYYY-MM-DDTHH:MM");
        }

        private LocalDateTime parseDateTime(String value, String errorMessage) {
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(errorMessage);
            }
        }

        String resolvePatientId() {
            if (patients.isEmpty() || patientCombo.getSelectedIndex() < 0) {
                return null;
            }
            return patients.get(patientCombo.getSelectedIndex()).getId();
        }

        String resolveDoctorId() {
            if (doctors.isEmpty() || doctorCombo.getSelectedIndex() < 0) {
                return null;
            }
            return doctors.get(doctorCombo.getSelectedIndex()).getId();
        }

        void setPatient(String patientId) {
            if (patientId == null) {
                return;
            }
            for (int i = 0; i < patients.size(); i++) {
                if (patients.get(i).getId().equals(patientId)) {
                    patientCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        void setDoctor(String doctorId) {
            if (doctorId == null) {
                return;
            }
            for (int i = 0; i < doctors.size(); i++) {
                if (doctors.get(i).getId().equals(doctorId)) {
                    doctorCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}
