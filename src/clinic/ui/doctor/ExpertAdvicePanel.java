package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.ExpertAdvice;
import clinic.model.Patient;
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

public class ExpertAdvicePanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public ExpertAdvicePanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "患者", "会诊", "建议日期", "建议概要", "随访计划"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("专家建议"));
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

        addButton.addActionListener(e -> createAdvice());
        editButton.addActionListener(e -> editAdvice());
        deleteButton.addActionListener(e -> deleteAdvice());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> patientNames = buildPatientNames();
            for (ExpertAdvice advice : context.getExpertAdviceService().listAll()) {
                model.addRow(new Object[]{
                    advice.getId(),
                    patientNames.getOrDefault(advice.getPatientId(), advice.getPatientId()),
                    advice.getSessionId(),
                    advice.getAdviceDate(),
                    advice.getAdviceSummary(),
                    advice.getFollowUpPlan()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载建议失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createAdvice() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            if (patients.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无患者信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            AdviceForm form = new AdviceForm(patients);
            if (form.showDialog(this, "新增建议")) {
                context.getExpertAdviceService().createAdvice(
                    form.sessionField.getText().trim().isEmpty() ? null : form.sessionField.getText().trim(),
                    form.resolvePatientId(),
                    form.doctorField.getText().trim().isEmpty() ? null : form.doctorField.getText().trim(),
                    form.parseDate(),
                    form.summaryField.getText().trim(),
                    form.followUpField.getText().trim()
                );
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editAdvice() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择建议", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        try {
            Optional<ExpertAdvice> optional = context.getExpertAdviceService().listAll().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
            if (optional.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到建议记录", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Patient> patients = context.getPatientService().listPatients();
            AdviceForm form = new AdviceForm(patients);
            ExpertAdvice advice = optional.get();
            form.setPatient(advice.getPatientId());
            form.sessionField.setText(advice.getSessionId() == null ? "" : advice.getSessionId());
            form.doctorField.setText(advice.getDoctorId() == null ? "" : advice.getDoctorId());
            form.dateField.setText(advice.getAdviceDate() == null ? "" : advice.getAdviceDate().toString());
            form.summaryField.setText(advice.getAdviceSummary());
            form.followUpField.setText(advice.getFollowUpPlan());
            if (form.showDialog(this, "编辑建议")) {
                ExpertAdvice updated = new ExpertAdvice(
                    advice.getId(),
                    form.sessionField.getText().trim().isEmpty() ? null : form.sessionField.getText().trim(),
                    form.resolvePatientId(),
                    form.doctorField.getText().trim().isEmpty() ? null : form.doctorField.getText().trim(),
                    form.parseDate(),
                    form.summaryField.getText().trim(),
                    form.followUpField.getText().trim()
                );
                context.getExpertAdviceService().updateAdvice(updated);
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAdvice() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的建议", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确认删除该建议?", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                context.getExpertAdviceService().deleteAdvice(model.getValueAt(row, 0).toString());
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

    private static class AdviceForm {
        final List<Patient> patients;
        final JComboBox<String> patientCombo;
        final JTextField sessionField = new JTextField();
        final JTextField doctorField = new JTextField();
        final JTextField dateField = new JTextField(LocalDate.now().toString());
        final JTextField summaryField = new JTextField();
        final JTextField followUpField = new JTextField();

        AdviceForm(List<Patient> patients) {
            this.patients = patients;
            patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
        }

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "患者", patientCombo,
                "会诊编号", sessionField,
                "医生编号", doctorField,
                "建议日期 (YYYY-MM-DD)", dateField,
                "建议概要", summaryField,
                "随访计划", followUpField
            };
            return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        String resolvePatientId() {
            if (patientCombo.getSelectedIndex() < 0) {
                throw new IllegalArgumentException("请选择患者");
            }
            return patients.get(patientCombo.getSelectedIndex()).getId();
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
    }
}
