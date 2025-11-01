package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.Consultation;
import clinic.model.Doctor;
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

public class ConsultationHistoryPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;

    public ConsultationHistoryPanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "医生", "摘要", "时间"}, 0) {
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
        top.add(new JLabel("历史问诊记录"));
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
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            Map<String, String> doctorNameMap = new HashMap<>();
            for (Doctor doctor : doctors) {
                doctorNameMap.put(doctor.getId(), doctor.getName());
            }
            for (Consultation consultation : context.getConsultationService().listByPatient(user.getId())) {
                model.addRow(new Object[]{
                    consultation.getId(),
                    doctorNameMap.getOrDefault(consultation.getDoctorId(), consultation.getDoctorId()),
                    consultation.getSummary(),
                    consultation.getCreatedAt()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载问诊记录失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
