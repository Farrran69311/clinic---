package clinic.ui;

import clinic.AppContext;
import clinic.model.Role;
import clinic.model.User;
import clinic.security.PermissionGuard;
import clinic.ui.doctor.AppointmentManagementPanel;
import clinic.ui.doctor.CalendarManagementPanel;
import clinic.ui.doctor.CaseLibraryPanel;
import clinic.ui.doctor.ConsultationManagementPanel;
import clinic.ui.doctor.ExpertAdvicePanel;
import clinic.ui.doctor.ExpertSessionManagementPanel;
import clinic.ui.doctor.InsightAssistantPanel;
import clinic.ui.doctor.MedicineManagementPanel;
import clinic.ui.doctor.PatientManagementPanel;
import clinic.ui.doctor.WorkProgressPanel;
import clinic.ui.patient.ConsultationHistoryPanel;
import clinic.ui.patient.PatientAppointmentPanel;
import clinic.ui.patient.PatientCaseRecordPanel;
import clinic.ui.patient.PatientDepartmentBookingPanel;
import clinic.ui.patient.PatientExpertAdvicePanel;
import clinic.ui.patient.PatientSchedulePanel;
import clinic.ui.patient.PatientWorkProgressPanel;
import clinic.ui.patient.PharmacyStatusPanel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private final AppContext context;
    private final User user;
    private final List<Refreshable> refreshables = new ArrayList<>();
    private Timer autoRefreshTimer;

    public MainFrame(AppContext context, User user) {
        super("医疗诊所管理系统");
        this.context = context;
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("医疗诊所管理系统 - 当前用户：" + user.getUsername() + " (" + user.getRole() + ")", SwingConstants.CENTER);
        header.add(title, BorderLayout.CENTER);
        JButton refreshButton = new JButton("立即刷新");
        refreshButton.addActionListener(this::handleManualRefresh);
        header.add(refreshButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        if (user.getRole() == Role.PATIENT) {
            add(buildPatientTabs(), BorderLayout.CENTER);
        } else {
            add(buildDoctorTabs(), BorderLayout.CENTER);
        }

        startAutoRefresh();
        refreshAllPanels();
    }

    private JPanel buildPatientTabs() {
        JTabbedPane tabs = new JTabbedPane();
        WelcomePanel welcomePanel = new WelcomePanel(context, user);
        registerTab(tabs, "首页概览", welcomePanel);
        PatientDepartmentBookingPanel departmentPanel = new PatientDepartmentBookingPanel(context, user);
        registerTab(tabs, "科室预约", departmentPanel);
        PatientAppointmentPanel appointmentPanel = new PatientAppointmentPanel(context, user);
        registerTab(tabs, "预约挂号", appointmentPanel);
        ConsultationHistoryPanel consultationHistoryPanel = new ConsultationHistoryPanel(context, user);
        registerTab(tabs, "问诊记录", consultationHistoryPanel);
        PharmacyStatusPanel pharmacyStatusPanel = new PharmacyStatusPanel(context, user);
        registerTab(tabs, "药房状态", pharmacyStatusPanel);
        PatientExpertAdvicePanel expertAdvicePanel = new PatientExpertAdvicePanel(context, user);
        registerTab(tabs, "专家建议", expertAdvicePanel);
        PatientWorkProgressPanel workProgressPanel = new PatientWorkProgressPanel(context, user);
        registerTab(tabs, "诊疗进度", workProgressPanel);
        PatientSchedulePanel schedulePanel = new PatientSchedulePanel(context, user);
        registerTab(tabs, "个人日程", schedulePanel);
        PatientCaseRecordPanel caseRecordPanel = new PatientCaseRecordPanel(context, user);
        registerTab(tabs, "个人病例", caseRecordPanel);
        JPanel container = new JPanel(new BorderLayout());
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildDoctorTabs() {
        JTabbedPane tabs = new JTabbedPane();
        WelcomePanel welcomePanel = new WelcomePanel(context, user);
        registerTab(tabs, "首页概览", welcomePanel);
        PermissionGuard permissionGuard = new PermissionGuard(context, user);
        PatientManagementPanel patientPanel = new PatientManagementPanel(context);
        registerTab(tabs, "患者管理", patientPanel);
        AppointmentManagementPanel appointmentPanel = new AppointmentManagementPanel(context, permissionGuard);
        registerTab(tabs, "预约管理", appointmentPanel);
        ConsultationManagementPanel consultationPanel = new ConsultationManagementPanel(context, permissionGuard);
        registerTab(tabs, "问诊记录", consultationPanel);
        MedicineManagementPanel medicinePanel = new MedicineManagementPanel(context);
        registerTab(tabs, "药房管理", medicinePanel);
        ExpertSessionManagementPanel expertSessionPanel = new ExpertSessionManagementPanel(context, permissionGuard);
        registerTab(tabs, "专家会诊", expertSessionPanel);
        CaseLibraryPanel caseLibraryPanel = new CaseLibraryPanel(context);
        registerTab(tabs, "病例库", caseLibraryPanel);
        WorkProgressPanel workProgressPanel = new WorkProgressPanel(context, permissionGuard);
        registerTab(tabs, "工作进度", workProgressPanel);
        CalendarManagementPanel calendarPanel = new CalendarManagementPanel(context, permissionGuard);
        registerTab(tabs, "工作日程", calendarPanel);
        ExpertAdvicePanel expertAdvicePanel = new ExpertAdvicePanel(context, permissionGuard);
        registerTab(tabs, "专家建议", expertAdvicePanel);
        InsightAssistantPanel insightPanel = new InsightAssistantPanel(context);
        registerTab(tabs, "智能助手", insightPanel);
        JPanel container = new JPanel(new BorderLayout());
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private void registerTab(JTabbedPane tabs, String title, JPanel panel) {
        tabs.addTab(title, panel);
        if (panel instanceof Refreshable refreshable) {
            refreshables.add(refreshable);
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        autoRefreshTimer = new Timer(15000, this::handleAutoRefresh);
        autoRefreshTimer.setInitialDelay(15000);
        autoRefreshTimer.start();
    }

    private void handleManualRefresh(ActionEvent event) {
        refreshAllPanels();
    }

    private void handleAutoRefresh(ActionEvent event) {
        refreshAllPanels();
    }

    private void refreshAllPanels() {
        for (Refreshable refreshable : refreshables) {
            try {
                refreshable.refreshData();
            } catch (Exception ex) {
                System.err.println("刷新面板失败: " + ex.getMessage());
            }
        }
    }

    @Override
    public void dispose() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        super.dispose();
    }
}
