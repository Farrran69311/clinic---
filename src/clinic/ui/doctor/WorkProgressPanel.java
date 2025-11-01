package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Doctor;
import clinic.model.Patient;
import clinic.model.WorkProgress;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;

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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkProgressPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public WorkProgressPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "患者", "描述", "状态", "更新日期", "责任医生"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    table = new JTable(model);
    TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("工作进度"));
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

        addButton.addActionListener(e -> createProgress());
        editButton.addActionListener(e -> editProgress());
        deleteButton.addActionListener(e -> deleteProgress());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> patientNames = buildPatientNames();
            Map<String, String> doctorNames = buildDoctorNames();
            for (WorkProgress progress : context.getWorkProgressService().listAll()) {
                model.addRow(new Object[]{
                    progress.getId(),
                    patientNames.getOrDefault(progress.getPatientId(), progress.getPatientId()),
                    progress.getDescription(),
                    progress.getStatus(),
                    progress.getLastUpdated(),
                    doctorNames.getOrDefault(progress.getOwnerDoctorId(), progress.getOwnerDoctorId())
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载进度失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createProgress() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            if (patients.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无患者信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ProgressForm form = new ProgressForm(patients, doctors);
            if (form.showDialog(this, "新增进度")) {
                context.getWorkProgressService().createProgress(
                    form.resolvePatientId(),
                    form.descriptionField.getText().trim(),
                    form.statusField.getText().trim().isEmpty() ? "ONGOING" : form.statusField.getText().trim(),
                    form.parseDate(),
                    form.resolveDoctorId()
                );
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editProgress() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择进度", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        try {
            Optional<WorkProgress> optional = context.getWorkProgressService().listAll().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
            if (optional.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到进度记录", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            ProgressForm form = new ProgressForm(patients, doctors);
            WorkProgress progress = optional.get();
            form.setPatient(progress.getPatientId());
            form.setDoctor(progress.getOwnerDoctorId());
            form.descriptionField.setText(progress.getDescription());
            form.statusField.setText(progress.getStatus());
            form.dateField.setText(progress.getLastUpdated() == null ? "" : progress.getLastUpdated().toString());
            if (form.showDialog(this, "编辑进度")) {
                WorkProgress updated = new WorkProgress(
                    progress.getId(),
                    form.resolvePatientId(),
                    form.descriptionField.getText().trim(),
                    form.statusField.getText().trim().isEmpty() ? progress.getStatus() : form.statusField.getText().trim(),
                    form.parseDate(),
                    form.resolveDoctorId()
                );
                context.getWorkProgressService().updateProgress(updated);
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProgress() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的进度", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确认删除该进度?", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                context.getWorkProgressService().deleteProgress(model.getValueAt(row, 0).toString());
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

    private static class ProgressForm {
        final List<Patient> patients;
        final List<Doctor> doctors;
        final JComboBox<String> patientCombo;
        final JComboBox<String> doctorCombo;
        final JTextField descriptionField = new JTextField();
        final JTextField statusField = new JTextField("ONGOING");
        final JTextField dateField = new JTextField(LocalDate.now().toString());

        ProgressForm(List<Patient> patients, List<Doctor> doctors) {
            this.patients = patients;
            this.doctors = doctors;
            patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
            doctorCombo = new JComboBox<>(doctors.stream().map(Doctor::getName).toArray(String[]::new));
        }

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "患者", patientCombo,
                "责任医生", doctorCombo,
                "描述", descriptionField,
                "状态", statusField,
                "更新日期 (YYYY-MM-DD)", dateField
            };
            return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        String resolvePatientId() {
            if (patientCombo.getSelectedIndex() < 0) {
                throw new IllegalArgumentException("请选择患者");
            }
            return patients.get(patientCombo.getSelectedIndex()).getId();
        }

        String resolveDoctorId() {
            if (doctors.isEmpty() || doctorCombo.getSelectedIndex() < 0) {
                return null;
            }
            return doctors.get(doctorCombo.getSelectedIndex()).getId();
        }

        LocalDate parseDate() {
            String value = dateField.getText().trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("日期格式应为YYYY-MM-DD");
            }
        }

        void setPatient(String patientId) {
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
