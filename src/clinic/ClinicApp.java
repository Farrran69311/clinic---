package clinic;

import clinic.model.User;
import clinic.ui.LoginFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;

public class ClinicApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppContext context = new AppContext(Paths.get("data"));
            seedData(context);
            LoginFrame frame = new LoginFrame(context, ClinicApp::onLoginSuccess);
            frame.setVisible(true);
        });
    }

    private static void onLoginSuccess(AppContext context, User user, LoginFrame loginFrame) {
        loginFrame.dispose();
        clinic.ui.MainFrame mainFrame = new clinic.ui.MainFrame(context, user);
        mainFrame.setVisible(true);
    }

    private static void seedData(AppContext context) {
        try {
            if (!context.getAuthService().userExists("doctor")) {
                context.getAuthService().createDoctorAccount("doctor", "doctor123");
            }
            if (context.getDoctorService().listDoctors().isEmpty()) {
                context.getDoctorService().createDoctor("张医生", "全科", "010-12345678", "周一至周五 09:00-17:00");
            }
        } catch (Exception ex) {
            System.err.println("初始化数据失败: " + ex.getMessage());
        }
    }
}
