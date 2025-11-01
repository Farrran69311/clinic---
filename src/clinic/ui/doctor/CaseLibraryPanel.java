package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.CaseRecord;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CaseLibraryPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public CaseLibraryPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "患者", "标题", "标签", "附件", "摘要"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    table = new JTable(model);
    TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("病例库"));
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

        addButton.addActionListener(e -> createRecord());
        editButton.addActionListener(e -> editRecord());
        deleteButton.addActionListener(e -> deleteRecord());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> patientNames = buildPatientNames();
            for (CaseRecord record : context.getCaseRecordService().listAll()) {
                model.addRow(new Object[]{
                    record.getId(),
                    patientNames.getOrDefault(record.getPatientId(), record.getPatientId()),
                    record.getTitle(),
                    record.getTags(),
                    record.getAttachmentPath(),
                    record.getSummary()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载病例失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createRecord() {
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            if (patients.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无患者信息", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            RecordForm form = new RecordForm(patients);
            if (form.showDialog(this, "新增病例")) {
                context.getCaseRecordService().createCaseRecord(
                    form.resolvePatientId(),
                    form.titleField.getText().trim(),
                    form.summaryField.getText().trim(),
                    form.tagsField.getText().trim(),
                    form.attachmentField.getText().trim()
                );
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择病例", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        try {
            Optional<CaseRecord> optional = context.getCaseRecordService().listAll().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
            if (optional.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到病例记录", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Patient> patients = context.getPatientService().listPatients();
            RecordForm form = new RecordForm(patients);
            CaseRecord record = optional.get();
            form.setPatient(record.getPatientId());
            form.titleField.setText(record.getTitle());
            form.summaryField.setText(record.getSummary());
            form.tagsField.setText(record.getTags());
            form.attachmentField.setText(record.getAttachmentPath() == null ? "" : record.getAttachmentPath());
            if (form.showDialog(this, "编辑病例")) {
                CaseRecord updated = new CaseRecord(
                    record.getId(),
                    form.resolvePatientId(),
                    form.titleField.getText().trim(),
                    form.summaryField.getText().trim(),
                    form.tagsField.getText().trim(),
                    form.attachmentField.getText().trim().isEmpty() ? null : form.attachmentField.getText().trim()
                );
                context.getCaseRecordService().updateCaseRecord(updated);
                refreshData();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的病例", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确认删除该病例?", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                context.getCaseRecordService().deleteCaseRecord(model.getValueAt(row, 0).toString());
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

    private static class RecordForm {
        final JComboBox<String> patientCombo;
        final List<Patient> patients;
        final JTextField titleField = new JTextField();
        final JTextField tagsField = new JTextField();
        final JTextField attachmentField = new JTextField();
        final JTextField summaryField = new JTextField();

        RecordForm(List<Patient> patients) {
            this.patients = patients;
            patientCombo = new JComboBox<>(patients.stream().map(Patient::getName).toArray(String[]::new));
        }

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "患者", patientCombo,
                "标题", titleField,
                "标签", tagsField,
                "附件路径", attachmentField,
                "摘要", summaryField
            };
            return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
        }

        String resolvePatientId() {
            if (patientCombo.getSelectedIndex() < 0) {
                throw new IllegalArgumentException("请选择患者");
            }
            return patients.get(patientCombo.getSelectedIndex()).getId();
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
