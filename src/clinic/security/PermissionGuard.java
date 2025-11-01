package clinic.security;

import clinic.AppContext;
import clinic.model.Doctor;
import clinic.model.Role;
import clinic.model.User;
import clinic.util.DoctorMatcher;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class PermissionGuard {
    private final AppContext context;
    private final User currentUser;
    private final Set<String> approvedDoctorIds = new HashSet<>();
    private Optional<Doctor> cachedCurrentDoctor;

    public PermissionGuard(AppContext context, User currentUser) {
        this.context = context;
        this.currentUser = currentUser;
    }

    public Role getCurrentRole() {
        return currentUser.getRole();
    }

    public boolean ensureDoctorAccess(Component parent, String doctorReference, String actionLabel) {
        if (doctorReference == null || doctorReference.isBlank()) {
            return true;
        }
        try {
            return evaluateAccess(parent, doctorReference.trim(), actionLabel);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "验证医生权限失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean ensureDoctorAccess(Component parent, Doctor doctor, String actionLabel) {
        if (doctor == null) {
            return true;
        }
        return ensureDoctorAccess(parent, doctor.getId(), actionLabel);
    }

    private boolean evaluateAccess(Component parent, String reference, String actionLabel) throws IOException {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        Optional<Doctor> targetDoctor = doctors.stream()
            .filter(doc -> DoctorMatcher.matches(reference, doc))
            .findFirst();
        if (targetDoctor.isPresent() && approvedDoctorIds.contains(targetDoctor.get().getId())) {
            return true;
        }
        Optional<Doctor> currentDoctor = getCurrentDoctor(doctors);
        if (currentDoctor.isPresent() && targetDoctor.isPresent() && currentDoctor.get().getId().equals(targetDoctor.get().getId())) {
            return true;
        }
        return promptForVerification(parent, doctors, targetDoctor, reference, actionLabel);
    }

    private Optional<Doctor> getCurrentDoctor(List<Doctor> doctors) {
        if (currentUser.getRole() != Role.DOCTOR) {
            return Optional.empty();
        }
        if (cachedCurrentDoctor != null) {
            return cachedCurrentDoctor;
        }
        Optional<Doctor> resolved = resolveDoctorForUser(doctors, currentUser);
        cachedCurrentDoctor = resolved;
        resolved.ifPresent(doctor -> approvedDoctorIds.add(doctor.getId()));
        return resolved;
    }

    private boolean promptForVerification(Component parent, List<Doctor> doctors, Optional<Doctor> targetDoctor,
                                           String reference, String actionLabel) throws IOException {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        if (actionLabel != null && !actionLabel.isBlank()) {
            panel.add(new JLabel("操作: " + actionLabel));
        }
        if (targetDoctor.isPresent()) {
            panel.add(new JLabel("目标医生: " + targetDoctor.get().getName()));
        } else {
            panel.add(new JLabel("目标医生标识: " + reference));
        }
        panel.add(new JLabel("医生账号"));
        panel.add(usernameField);
        panel.add(new JLabel("登录密码"));
        panel.add(passwordField);

        int option = JOptionPane.showConfirmDialog(parent, panel, "跨账号修改验证", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] password = passwordField.getPassword();
        if (option != JOptionPane.OK_OPTION) {
            Arrays.fill(password, '\0');
            return false;
        }
        try {
            Optional<User> loginResult = context.getAuthService().login(usernameField.getText().trim(), new String(password));
            if (loginResult.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "账号或密码错误", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            User verifiedUser = loginResult.get();
            if (verifiedUser.getRole() != Role.DOCTOR) {
                JOptionPane.showMessageDialog(parent, "必须使用医生账号完成验证", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            Optional<Doctor> verifiedDoctor = resolveDoctorForUser(doctors, verifiedUser);
            if (verifiedDoctor.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "该账号尚未关联医生档案，请检查医生信息表", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            Doctor doctor = verifiedDoctor.get();
            if (targetDoctor.isPresent() && !Objects.equals(targetDoctor.get().getId(), doctor.getId())) {
                JOptionPane.showMessageDialog(parent, "请输入目标医生 " + targetDoctor.get().getName() + " 的账号", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (targetDoctor.isEmpty() && !DoctorMatcher.matches(reference, doctor)) {
                JOptionPane.showMessageDialog(parent, "输入的医生账号与目标记录不一致", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            approvedDoctorIds.add(doctor.getId());
            return true;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Optional<Doctor> resolveDoctorForUser(List<Doctor> doctors, User user) {
        if (user == null) {
            return Optional.empty();
        }
        for (Doctor doctor : doctors) {
            if (Objects.equals(doctor.getId(), user.getId())) {
                return Optional.of(doctor);
            }
        }
        for (Doctor doctor : doctors) {
            if (doctor.getName().equalsIgnoreCase(user.getUsername())) {
                return Optional.of(doctor);
            }
        }
        String normalized = DoctorMatcher.normalizeName(user.getUsername());
        if (!normalized.isBlank()) {
            for (Doctor doctor : doctors) {
                if (DoctorMatcher.normalizeName(doctor.getName()).equalsIgnoreCase(normalized)) {
                    return Optional.of(doctor);
                }
            }
        }
        return Optional.empty();
    }
}
