package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.Doctor;
import clinic.model.User;
import clinic.model.WorkProgress;
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
import java.util.HashMap;
import java.util.Map;

public class PatientWorkProgressPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;

    public PatientWorkProgressPanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"编号", "描述", "状态", "更新日期", "责任医生"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("诊疗进度"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        add(header, BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                String description = model.getValueAt(table.getSelectedRow(), 1).toString();
                JOptionPane.showMessageDialog(this, description, "进度详情", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> doctorNames = buildDoctorNames();
            for (WorkProgress progress : context.getWorkProgressService().listByPatient(user.getId())) {
                model.addRow(new Object[]{
                    progress.getId(),
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

    private Map<String, String> buildDoctorNames() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (Doctor doctor : context.getDoctorService().listDoctors()) {
            map.put(doctor.getId(), doctor.getName());
        }
        return map;
    }
}
