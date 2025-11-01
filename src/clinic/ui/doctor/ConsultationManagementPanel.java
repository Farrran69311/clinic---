package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Consultation;
import clinic.model.Doctor;
import clinic.model.Medicine;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.HashMap;

public class ConsultationManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final PermissionGuard permissionGuard;
    private final DefaultTableModel model;
    private final JTable table;
    private final Map<String, Consultation> consultationIndex = new HashMap<>();

    public ConsultationManagementPanel(AppContext context, PermissionGuard permissionGuard) {
        this.context = context;
        this.permissionGuard = permissionGuard;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "患者", "医生", "预约", "摘要", "时间"}, 0) {
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
        top.add(new JLabel("问诊记录管理"));
        top.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        top.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("新增问诊");
        JButton editButton = new JButton("编辑摘要");
        JButton prescriptionButton = new JButton("开立处方");
        JButton deleteButton = new JButton("删除");
        controls.add(addButton);
        controls.add(editButton);
        controls.add(prescriptionButton);
        controls.add(deleteButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> createConsultation());
        editButton.addActionListener(e -> editConsultation());
        prescriptionButton.addActionListener(e -> createPrescription());
        deleteButton.addActionListener(e -> deleteConsultation());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            consultationIndex.clear();
            Map<String, String> patientNames = new HashMap<>();
            for (Patient patient : context.getPatientService().listPatients()) {
                patientNames.put(patient.getId(), patient.getName());
            }
            Map<String, String> doctorNames = new HashMap<>();
            for (Doctor doctor : context.getDoctorService().listDoctors()) {
                doctorNames.put(doctor.getId(), doctor.getName());
            }
            Map<String, String> appointmentDesc = new HashMap<>();
            for (Appointment appointment : context.getAppointmentService().listAppointments()) {
                appointmentDesc.put(appointment.getId(), appointment.getDateTime().toString());
            }
            for (Consultation consultation : context.getConsultationService().listConsultations()) {
                consultationIndex.put(consultation.getId(), consultation);
                model.addRow(new Object[]{
                    consultation.getId(),
                    patientNames.getOrDefault(consultation.getPatientId(), consultation.getPatientId()),
                    doctorNames.getOrDefault(consultation.getDoctorId(), consultation.getDoctorId()),
                    appointmentDesc.getOrDefault(consultation.getAppointmentId(), consultation.getAppointmentId()),
                    consultation.getSummary(),
                    consultation.getCreatedAt()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载问诊记录失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createConsultation() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            List<Appointment> appointments = context.getAppointmentService().listAppointments();
            if (patients.isEmpty() || doctors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先准备患者和医生信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<String> patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
            JComboBox<String> doctorCombo = new JComboBox<>(doctors.stream().map(Doctor::getName).toArray(String[]::new));
            List<Appointment> filteredAppointments = appointments.stream()
                .filter(a -> a.getStatus().equalsIgnoreCase("CONFIRMED") || a.getStatus().equalsIgnoreCase("COMPLETED") || a.getStatus().equalsIgnoreCase("PENDING"))
                .collect(Collectors.toList());
            JComboBox<String> appointmentCombo = new JComboBox<>(filteredAppointments.stream()
                .map(a -> a.getId() + " - " + a.getDateTime())
                .toArray(String[]::new));
            JTextArea summaryArea = new JTextArea(5, 20);
            Object[] message = {
                "患者", patientCombo,
                "医生", doctorCombo,
                "关联预约", appointmentCombo,
                "问诊摘要", new JScrollPane(summaryArea)
            };
            int option = JOptionPane.showConfirmDialog(this, message, "新增问诊", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String patientId = patients.get(patientCombo.getSelectedIndex()).getId();
                String doctorId = doctors.get(doctorCombo.getSelectedIndex()).getId();
                if (!permissionGuard.ensureDoctorAccess(this, doctorId, "新增问诊")) {
                    return;
                }
                String appointmentId = filteredAppointments.isEmpty() ? null : filteredAppointments.get(appointmentCombo.getSelectedIndex()).getId();
                context.getConsultationService().createConsultation(
                    patientId,
                    doctorId,
                    appointmentId,
                    summaryArea.getText().trim(),
                    null
                );
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "写入失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editConsultation() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要编辑的问诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        Consultation consultation = consultationIndex.get(id);
        try {
            if (consultation == null) {
                consultation = context.getConsultationService().listConsultations().stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            }
            if (consultation != null && !permissionGuard.ensureDoctorAccess(this, consultation.getDoctorId(), "编辑问诊")) {
                return;
            }
            String summary = model.getValueAt(row, 4).toString();
            JTextArea area = new JTextArea(summary, 6, 20);
            int option = JOptionPane.showConfirmDialog(this, new JScrollPane(area), "编辑问诊摘要", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION && consultation != null) {
                context.getConsultationService().updateConsultation(new Consultation(
                    consultation.getId(),
                    consultation.getPatientId(),
                    consultation.getDoctorId(),
                    consultation.getAppointmentId(),
                    area.getText().trim(),
                    consultation.getPrescriptionId(),
                    consultation.getCreatedAt()
                ));
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteConsultation() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的问诊", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除该记录?", "确认", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                Consultation consultation = consultationIndex.get(model.getValueAt(row, 0).toString());
                if (consultation != null && !permissionGuard.ensureDoctorAccess(this, consultation.getDoctorId(), "删除问诊")) {
                    return;
                }
                context.getConsultationService().deleteConsultation(model.getValueAt(row, 0).toString());
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createPrescription() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择问诊记录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String consultationId = model.getValueAt(row, 0).toString();
        try {
            Consultation consultation = consultationIndex.get(consultationId);
            if (consultation != null && !permissionGuard.ensureDoctorAccess(this, consultation.getDoctorId(), "开立处方")) {
                return;
            }
            List<Medicine> medicines = context.getPharmacyService().listMedicines();
            if (medicines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先添加药品库存", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<String> medicineCombo = new JComboBox<>(medicines.stream().map(Medicine::getName).toArray(String[]::new));
            JTextField quantityField = new JTextField("1");
            JTextField usageField = new JTextField();
            Object[] message = {
                "药品", medicineCombo,
                "数量", quantityField,
                "用法", usageField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "开立处方", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "数量需为整数", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String medicineId = medicines.get(medicineCombo.getSelectedIndex()).getId();
                context.getPharmacyService().createPrescription(consultationId, medicineId, quantity, usageField.getText().trim());
                JOptionPane.showMessageDialog(this, "处方已生成", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "操作失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
