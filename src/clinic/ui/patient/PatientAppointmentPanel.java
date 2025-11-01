package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.Appointment;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientAppointmentPanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;
    private final JTable table;

    public PatientAppointmentPanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);
        model = new DefaultTableModel(new String[]{"编号", "医生", "时间", "状态", "备注"}, 0) {
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
        top.add(new JLabel("我的预约"));
        top.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        top.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("新增预约");
        JButton cancelButton = new JButton("取消预约");
        controls.add(addButton);
        controls.add(cancelButton);
        add(controls, BorderLayout.SOUTH);

        addButton.addActionListener(e -> openCreateDialog());
        cancelButton.addActionListener(e -> cancelSelected());

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
            for (Appointment appointment : context.getAppointmentService().listAppointments()) {
                if (!appointment.getPatientId().equals(user.getId())) {
                    continue;
                }
                model.addRow(new Object[]{
                    appointment.getId(),
                    doctorNameMap.getOrDefault(appointment.getDoctorId(), appointment.getDoctorId()),
                    appointment.getDateTime(),
                    appointment.getStatus(),
                    appointment.getNotes()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载预约失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCreateDialog() {
        try {
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            if (doctors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无医生可预约，请联系诊所", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] doctorNames = doctors.stream().map(Doctor::getName).toArray(String[]::new);
            String doctorName = (String) JOptionPane.showInputDialog(this, "选择医生", "预约挂号",
                JOptionPane.PLAIN_MESSAGE, null, doctorNames, doctorNames[0]);
            if (doctorName == null) {
                return;
            }
            String doctorId = doctors.stream()
                .filter(d -> d.getName().equals(doctorName))
                .map(Doctor::getId)
                .findFirst()
                .orElse(doctors.get(0).getId());
            JTextField datetimeField = new JTextField("2025-01-01T09:00");
            JTextField notesField = new JTextField();
            Object[] message = {
                "就诊时间 (YYYY-MM-DDTHH:MM)", datetimeField,
                "症状描述", notesField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "预约挂号", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(datetimeField.getText().trim());
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "时间格式错误", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                context.getAppointmentService().createAppointment(user.getId(), doctorId, dateTime, notesField.getText().trim());
                refreshData();
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "创建预约失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择要取消的预约", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        try {
            context.getAppointmentService().deleteAppointment(id);
            refreshData();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "取消失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
