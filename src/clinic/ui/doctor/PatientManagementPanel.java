package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Patient;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;

import javax.swing.JButton;
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
import java.util.List;

public class PatientManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel model;
    private final JTable table;

    public PatientManagementPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "姓名", "性别", "生日", "电话", "地址", "紧急联系人", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    table = new JTable(model);
    TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("患者档案"));
        top.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(20);
        TableUtils.installSearchFilter(table, searchField);
        top.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("新增患者");
        JButton editButton = new JButton("编辑");
        JButton deleteButton = new JButton("删除");
        controls.add(addButton);
        controls.add(editButton);
        controls.add(deleteButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> createPatient());
        editButton.addActionListener(e -> editPatient());
        deleteButton.addActionListener(e -> deletePatient());

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            List<Patient> patients = context.getPatientService().listPatients();
            for (Patient patient : patients) {
                model.addRow(new Object[]{
                    patient.getId(),
                    patient.getName(),
                    patient.getGender(),
                    patient.getBirthday(),
                    patient.getPhone(),
                    patient.getAddress(),
                    patient.getEmergencyContact(),
                    patient.getNotes()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载患者失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createPatient() {
        PatientForm form = new PatientForm();
        if (form.showDialog(this, "新增患者")) {
            try {
                context.getPatientService().createPatient(
                    form.nameField.getText().trim(),
                    form.genderField.getText().trim(),
                    form.parseBirthday(),
                    form.phoneField.getText().trim(),
                    form.addressField.getText().trim(),
                    form.emergencyField.getText().trim(),
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

    private void editPatient() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要编辑的患者", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        PatientForm form = new PatientForm();
        form.nameField.setText(model.getValueAt(row, 1).toString());
        form.genderField.setText(valueOrEmpty(model.getValueAt(row, 2)));
        form.birthdayField.setText(valueOrEmpty(model.getValueAt(row, 3)));
        form.phoneField.setText(valueOrEmpty(model.getValueAt(row, 4)));
        form.notesField.setText(valueOrEmpty(model.getValueAt(row, 7)));
        form.addressField.setText(valueOrEmpty(model.getValueAt(row, 5)));
        form.emergencyField.setText(valueOrEmpty(model.getValueAt(row, 6)));
        if (form.showDialog(this, "编辑患者")) {
            try {
                context.getPatientService().updatePatient(new Patient(
                    id,
                    form.nameField.getText().trim(),
                    form.genderField.getText().trim(),
                    form.parseBirthday(),
                    form.phoneField.getText().trim(),
                    form.addressField.getText().trim(),
                    form.emergencyField.getText().trim(),
                    form.notesField.getText().trim()
                ));
                refreshData();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deletePatient() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的患者", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除该患者?", "确认", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                context.getPatientService().deletePatient(id);
                refreshData();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class PatientForm {
        final JTextField nameField = new JTextField();
        final JTextField genderField = new JTextField();
        final JTextField birthdayField = new JTextField("1990-01-01");
        final JTextField phoneField = new JTextField();
        final JTextField addressField = new JTextField();
        final JTextField emergencyField = new JTextField();
        final JTextField notesField = new JTextField();

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "姓名", nameField,
                "性别", genderField,
                "出生日期 (YYYY-MM-DD)", birthdayField,
                "电话", phoneField,
                "地址", addressField,
                "紧急联系人", emergencyField,
                "备注", notesField
            };
            int option = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION);
            return option == JOptionPane.OK_OPTION;
        }

        LocalDate parseBirthday() {
            String text = birthdayField.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("生日格式应为YYYY-MM-DD");
            }
        }
    }
}
