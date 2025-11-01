package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Medicine;
import clinic.model.Prescription;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MedicineManagementPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final DefaultTableModel medicineModel;
    private final DefaultTableModel prescriptionModel;
    private final JTable medicineTable;
    private final JTable prescriptionTable;

    public MedicineManagementPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));

        medicineModel = new DefaultTableModel(new String[]{"编号", "名称", "规格", "库存", "单位", "有效期"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        prescriptionModel = new DefaultTableModel(new String[]{"处方编号", "咨询", "药品", "数量", "用法", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        medicineTable = new JTable(medicineModel);
        prescriptionTable = new JTable(prescriptionModel);

        JPanel medicinePanel = new JPanel(new BorderLayout());
        JPanel medicineHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        medicineHeader.add(new JLabel("药品库存"));
        medicineHeader.add(new JLabel("搜索:"));
        JTextField medicineSearch = new JTextField(14);
        TableUtils.installSearchFilter(medicineTable, medicineSearch);
        medicineHeader.add(medicineSearch);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        medicineHeader.add(refreshButton);
        medicinePanel.add(medicineHeader, BorderLayout.NORTH);
        medicinePanel.add(new JScrollPane(medicineTable), BorderLayout.CENTER);
        JPanel medicineControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addMed = new JButton("新增药品");
        JButton editMed = new JButton("编辑");
        JButton deleteMed = new JButton("删除");
        medicineControls.add(addMed);
        medicineControls.add(editMed);
        medicineControls.add(deleteMed);
        medicinePanel.add(medicineControls, BorderLayout.SOUTH);

        JPanel prescriptionPanel = new JPanel(new BorderLayout());
        JPanel prescriptionHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        prescriptionHeader.add(new JLabel("处方任务"));
        prescriptionHeader.add(new JLabel("搜索:"));
        JTextField prescriptionSearch = new JTextField(14);
        TableUtils.installSearchFilter(prescriptionTable, prescriptionSearch);
        prescriptionHeader.add(prescriptionSearch);
        prescriptionPanel.add(prescriptionHeader, BorderLayout.NORTH);
        prescriptionPanel.add(new JScrollPane(prescriptionTable), BorderLayout.CENTER);
        JPanel prescriptionControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton markDone = new JButton("标记为已发药");
        prescriptionControls.add(markDone);
        prescriptionPanel.add(prescriptionControls, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, medicinePanel, prescriptionPanel);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);

        addMed.addActionListener(e -> addMedicine());
        editMed.addActionListener(e -> editMedicine());
        deleteMed.addActionListener(e -> deleteMedicine());
        markDone.addActionListener(e -> markPrescriptionDone());

        refreshData();
    }

    @Override
    public void refreshData() {
        refreshMedicines();
        refreshPrescriptions();
    }

    private void refreshMedicines() {
        medicineModel.setRowCount(0);
        try {
            List<Medicine> medicines = context.getPharmacyService().listMedicines();
            for (Medicine medicine : medicines) {
                medicineModel.addRow(new Object[]{
                    medicine.getId(),
                    medicine.getName(),
                    medicine.getSpecification(),
                    medicine.getStock(),
                    medicine.getUnit(),
                    medicine.getExpiryDate()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载药品失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshPrescriptions() {
        prescriptionModel.setRowCount(0);
        try {
            List<Prescription> prescriptions = context.getPharmacyService().listPrescriptions();
            for (Prescription prescription : prescriptions) {
                prescriptionModel.addRow(new Object[]{
                    prescription.getId(),
                    prescription.getConsultationId(),
                    prescription.getMedicineId(),
                    prescription.getQuantity(),
                    prescription.getUsage(),
                    prescription.getStatus()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载处方失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addMedicine() {
        MedicineForm form = new MedicineForm();
        if (form.showDialog(this, "新增药品")) {
            try {
                context.getPharmacyService().addMedicine(
                    form.nameField.getText().trim(),
                    form.specField.getText().trim(),
                    form.parseStock(),
                    form.unitField.getText().trim(),
                    form.parseExpiry()
                );
                refreshMedicines();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要编辑的药品", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        MedicineForm form = new MedicineForm();
        form.nameField.setText(medicineModel.getValueAt(row, 1).toString());
        form.specField.setText(valueOrEmpty(medicineModel.getValueAt(row, 2)));
        form.stockField.setText(valueOrEmpty(medicineModel.getValueAt(row, 3)));
        form.unitField.setText(valueOrEmpty(medicineModel.getValueAt(row, 4)));
        form.expiryField.setText(valueOrEmpty(medicineModel.getValueAt(row, 5)));
        if (form.showDialog(this, "编辑药品")) {
            try {
                context.getPharmacyService().updateMedicine(new Medicine(
                    medicineModel.getValueAt(row, 0).toString(),
                    form.nameField.getText().trim(),
                    form.specField.getText().trim(),
                    form.parseStock(),
                    form.unitField.getText().trim(),
                    form.parseExpiry()
                ));
                refreshMedicines();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "保存失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的药品", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除该药品?", "确认", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                context.getPharmacyService().removeMedicine(medicineModel.getValueAt(row, 0).toString());
                refreshMedicines();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "删除失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void markPrescriptionDone() {
        int row = prescriptionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择处方", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = prescriptionModel.getValueAt(row, 0).toString();
        try {
            context.getPharmacyService().updatePrescriptionStatus(id, "DONE");
            refreshPrescriptions();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "更新失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class MedicineForm {
        final JTextField nameField = new JTextField();
        final JTextField specField = new JTextField();
        final JTextField stockField = new JTextField("0");
        final JTextField unitField = new JTextField();
        final JTextField expiryField = new JTextField("2025-12-31");

        boolean showDialog(JPanel parent, String title) {
            Object[] message = {
                "名称", nameField,
                "规格", specField,
                "库存", stockField,
                "单位", unitField,
                "有效期 (YYYY-MM-DD)", expiryField
            };
            int option = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION);
            return option == JOptionPane.OK_OPTION;
        }

        int parseStock() {
            try {
                return Integer.parseInt(stockField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("库存需为数字");
            }
        }

        LocalDate parseExpiry() {
            String text = expiryField.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("有效期格式应为YYYY-MM-DD");
            }
        }
    }
}
