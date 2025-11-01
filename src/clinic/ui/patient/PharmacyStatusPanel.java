package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.Consultation;
import clinic.model.Medicine;
import clinic.model.Prescription;
import clinic.model.User;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;
import clinic.ui.common.UIUtils;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PharmacyStatusPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;

    public PharmacyStatusPanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"处方编号", "药品", "数量", "用法", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(top);
        top.add(new JLabel("药房处方状态"));
        top.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        top.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            List<Consultation> consultations = context.getConsultationService().listByPatient(user.getId());
            Set<String> consultationIds = consultations.stream().map(Consultation::getId).collect(Collectors.toSet());
            Map<String, String> medicineNameMap = new HashMap<>();
            for (Medicine medicine : context.getPharmacyService().listMedicines()) {
                medicineNameMap.put(medicine.getId(), medicine.getName());
            }
            for (Prescription prescription : context.getPharmacyService().listPrescriptions()) {
                if (!consultationIds.contains(prescription.getConsultationId())) {
                    continue;
                }
                model.addRow(new Object[]{
                    prescription.getId(),
                    medicineNameMap.getOrDefault(prescription.getMedicineId(), prescription.getMedicineId()),
                    prescription.getQuantity(),
                    prescription.getUsage(),
                    prescription.getStatus()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载处方信息失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
