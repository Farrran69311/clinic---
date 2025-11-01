package clinic.ui;

import clinic.AppContext;
import clinic.model.User;
import clinic.service.AuthService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;

public class LoginFrame extends JFrame {
    public interface LoginSuccessListener {
        void onLoginSuccess(AppContext context, User user, LoginFrame loginFrame);
    }

    private final AppContext context;
    private final LoginSuccessListener listener;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    public LoginFrame(AppContext context, LoginSuccessListener listener) {
        super("医疗诊所管理系统 - 登录");
        this.context = context;
        this.listener = listener;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("医疗诊所管理系统", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(18f));
        content.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("用户名"));
        form.add(usernameField);
        form.add(new JLabel("密码"));
        form.add(passwordField);
        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton loginButton = new JButton("登录");
        JButton registerButton = new JButton("注册患者账号");
        buttons.add(loginButton);
        buttons.add(registerButton);
        content.add(buttons, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> doLogin());
        registerButton.addActionListener(e -> openRegisterDialog());

        setContentPane(content);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AuthService authService = context.getAuthService();
        try {
            authService.login(username, password)
                .ifPresentOrElse(user -> listener.onLoginSuccess(context, user, this),
                    () -> JOptionPane.showMessageDialog(this, "用户名或密码错误", "错误", JOptionPane.ERROR_MESSAGE));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取用户数据失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, context.getAuthService());
        dialog.setVisible(true);
    }
}
