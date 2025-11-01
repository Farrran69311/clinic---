package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Doctor;
import clinic.model.Patient;
import clinic.security.PermissionGuard;
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

public class AppointmentManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final PermissionGuard permissionGuard;
    private final DefaultTableModel model;
    private final JTable table;
    private final Map<String, Appointment> appointmentIndex = new HashMap<>();

    public AppointmentManagementPanel(AppContext context, PermissionGuard permissionGuard) {
        this.context = context;
        this.permissionGuard = permissionGuard;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "患者", "医生", "时间", "状态", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(top);
        top.add(new JLabel("预约管理"));
        top.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        top.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("新增");
        JButton updateStatusButton = new JButton("更新状态");
        JButton deleteButton = new JButton("删除");
        controls.add(addButton);
        controls.add(updateStatusButton);
        controls.add(deleteButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> createAppointment());
        updateStatusButton.addActionListener(e -> updateStatus());
        deleteButton.addActionListener(e -> deleteAppointment());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> patientNames = new HashMap<>();
            for (Patient patient : context.getPatientService().listPatients()) {
                patientNames.put(patient.getId(), patient.getName());
            }
            appointmentIndex.clear();
            Map<String, String> doctorNames = new HashMap<>();
            for (Doctor doctor : context.getDoctorService().listDoctors()) {
                doctorNames.put(doctor.getId(), doctor.getName());
            }
            for (Appointment appointment : context.getAppointmentService().listAppointments()) {
                appointmentIndex.put(appointment.getId(), appointment);
                model.addRow(new Object[]{
                    appointment.getId(),
                    patientNames.getOrDefault(appointment.getPatientId(), appointment.getPatientId()),
                    doctorNames.getOrDefault(appointment.getDoctorId(), appointment.getDoctorId()),
                    appointment.getDateTime(),
                    appointment.getStatus(),
                    appointment.getNotes()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载预约失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createAppointment() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            if (patients.isEmpty() || doctors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先确保有患者和医生信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<String> patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
            JComboBox<String> doctorCombo = new JComboBox<>(doctors.stream().map(Doctor::getName).toArray(String[]::new));
            JTextField datetimeField = new JTextField("2025-01-01T09:00");
            JTextField notesField = new JTextField();
            Object[] message = {
                "患者", patientCombo,
                "医生", doctorCombo,
                "时间 (YYYY-MM-DDTHH:MM)", datetimeField,
                "备注", notesField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "新增预约", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(datetimeField.getText().trim());
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "时间格式错误", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String patientId = patients.get(patientCombo.getSelectedIndex()).getId();
                String doctorId = doctors.get(doctorCombo.getSelectedIndex()).getId();
                if (!permissionGuard.ensureDoctorAccess(this, doctorId, "新增预约")) {
                    return;
                }
                context.getAppointmentService().createAppointment(patientId, doctorId, dateTime, notesField.getText().trim());
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择预约", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String appointmentId = model.getValueAt(row, 0).toString();
        Appointment appointment = appointmentIndex.get(appointmentId);
        if (appointment != null && !permissionGuard.ensureDoctorAccess(this, appointment.getDoctorId(), "更新预约状态")) {
            return;
        }
        String[] options = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"};
        String current = model.getValueAt(row, 4).toString();
        String status = (String) JOptionPane.showInputDialog(this, "选择新的状态", "更新预约状态",
            JOptionPane.PLAIN_MESSAGE, null, options, current);
        if (status != null) {
            try {
                context.getAppointmentService().updateStatus(appointmentId, status);
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "更新失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteAppointment() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的预约", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String appointmentId = model.getValueAt(row, 0).toString();
        Appointment appointment = appointmentIndex.get(appointmentId);
        if (appointment != null && !permissionGuard.ensureDoctorAccess(this, appointment.getDoctorId(), "删除预约")) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除该预约?", "确认", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                context.getAppointmentService().deleteAppointment(appointmentId);
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
