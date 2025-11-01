package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.CalendarEvent;
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
import java.util.Map;

public class PatientSchedulePanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;

    public PatientSchedulePanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "标题", "开始时间", "结束时间", "责任医生", "地点", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        TableUtils.installRowPreview(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("个人日程"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        add(header, BorderLayout.NORTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            Map<String, String> doctorNames = buildDoctorNames();
            for (CalendarEvent event : context.getCalendarEventService().listAll()) {
                if (user.getId().equals(event.getRelatedPatientId())) {
                    model.addRow(new Object[]{
                        event.getId(),
                        event.getTitle(),
                        event.getStart(),
                        event.getEnd(),
                        doctorNames.getOrDefault(event.getOwnerDoctorId(), event.getOwnerDoctorId()),
                        event.getLocation(),
                        event.getNotes()
                    });
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载日程失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
