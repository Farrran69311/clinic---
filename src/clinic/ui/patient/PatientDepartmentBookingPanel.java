package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Doctor;
import clinic.model.User;
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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PatientDepartmentBookingPanel extends JPanel implements Refreshable {
    private static final String ALL_DEPARTMENTS = "全部科室";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> departmentCombo = new JComboBox<>();
    private final JTextField searchField = new JTextField(18);

    public PatientDepartmentBookingPanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);

        model = new DefaultTableModel(new String[]{
            "编号", "医生", "科室", "职称", "级别", "评分", "擅长领域", "预约数量", "最近预约", "出诊时间"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        TableUtils.installRowPreview(table);
        TableUtils.installSearchFilter(table, searchField);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(buildTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        refreshAll();
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(panel);
        panel.add(new JLabel("科室:"));
        panel.add(departmentCombo);
        departmentCombo.addActionListener(e -> refreshTable());
        panel.add(new JLabel("搜索:"));
        panel.add(searchField);
        JButton refreshButton = new JButton("刷新数据");
        refreshButton.addActionListener(e -> refreshAll());
        panel.add(refreshButton);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bookButton = new JButton("预约该医生");
        bookButton.addActionListener(e -> openBookingDialog());
        panel.add(bookButton);
        return panel;
    }

    @Override
    public void refreshData() {
        refreshAll();
    }

    private void refreshAll() {
        try {
            populateDepartments();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "刷新失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateDepartments() throws Exception {
        String previous = (String) departmentCombo.getSelectedItem();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        Set<String> departments = doctors.stream()
            .map(Doctor::getDepartment)
            .filter(dep -> dep != null && !dep.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        departmentCombo.removeAllItems();
        departmentCombo.addItem(ALL_DEPARTMENTS);
        for (String dep : departments) {
            departmentCombo.addItem(dep);
        }
        if (previous != null) {
            departmentCombo.setSelectedItem(previous);
        }
        if (departmentCombo.getSelectedIndex() < 0 && departmentCombo.getItemCount() > 0) {
            departmentCombo.setSelectedIndex(0);
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        try {
            List<Doctor> doctors = context.getDoctorService().listDoctors();
            Map<String, AppointmentStats> stats = analyzeAppointments(context.getAppointmentService().listAppointments());
            String selectedDepartment = (String) departmentCombo.getSelectedItem();
            if (selectedDepartment == null) {
                selectedDepartment = ALL_DEPARTMENTS;
            }
            for (Doctor doctor : doctors) {
                if (!ALL_DEPARTMENTS.equals(selectedDepartment)) {
                    if (doctor.getDepartment() == null || !doctor.getDepartment().equals(selectedDepartment)) {
                        continue;
                    }
                }
                AppointmentStats doctorStats = stats.getOrDefault(doctor.getId(), AppointmentStats.empty());
                model.addRow(new Object[]{
                    doctor.getId(),
                    doctor.getName(),
                    safeText(doctor.getDepartment()),
                    safeText(doctor.getTitle()),
                    safeText(doctor.getLevel()),
                    formatRating(doctor.getRating()),
                    safeText(doctor.getSpecialties()),
                    doctorStats.upcomingCount(),
                    doctorStats.nextAppointment() == null ? "暂无" : DATE_TIME_FORMATTER.format(doctorStats.nextAppointment()),
                    safeText(doctor.getSchedule())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载数据失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBookingDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一位医生", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String doctorId = model.getValueAt(modelRow, 0).toString();
        String doctorName = model.getValueAt(modelRow, 1).toString();
        String schedule = model.getValueAt(modelRow, 9).toString();

        JTextField datetimeField = new JTextField(suggestAppointmentTime());
        JTextField notesField = new JTextField();
        Object[] message = {
            "医生: " + doctorName,
            "出诊时间: " + schedule,
            "预约时间 (YYYY-MM-DDTHH:MM)", datetimeField,
            "症状描述", notesField
        };
        int option = JOptionPane.showConfirmDialog(this, message, "预约挂号", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return;
        }
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(datetimeField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "时间格式错误", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            context.getAppointmentService().createAppointment(user.getId(), doctorId, dateTime, notesField.getText().trim());
            JOptionPane.showMessageDialog(this, "预约提交成功，您可在“预约挂号”查看进度。", "提示", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "预约失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, AppointmentStats> analyzeAppointments(List<Appointment> appointments) {
        Map<String, List<Appointment>> byDoctor = new HashMap<>();
        for (Appointment appointment : appointments) {
            byDoctor.computeIfAbsent(appointment.getDoctorId(), key -> new ArrayList<>()).add(appointment);
        }
        Map<String, AppointmentStats> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, List<Appointment>> entry : byDoctor.entrySet()) {
            List<Appointment> docAppointments = entry.getValue();
            long upcoming = docAppointments.stream()
                .filter(a -> !a.getDateTime().isBefore(now))
                .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .count();
            LocalDateTime next = docAppointments.stream()
                .filter(a -> !a.getDateTime().isBefore(now))
                .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .map(Appointment::getDateTime)
                .min(Comparator.naturalOrder())
                .orElse(null);
            stats.put(entry.getKey(), new AppointmentStats(upcoming, next));
        }
        return stats;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "暂无" : value;
    }

    private String formatRating(Double rating) {
        if (rating == null) {
            return "暂无";
        }
        return String.format(Locale.ROOT, "%.1f", rating);
    }

    private String suggestAppointmentTime() {
        LocalDate date = LocalDate.now().plusDays(1);
        return date.atTime(9, 0).toString();
    }

    private record AppointmentStats(long upcomingCount, LocalDateTime nextAppointment) {
        private static AppointmentStats empty() {
            return new AppointmentStats(0, null);
        }
    }
}
