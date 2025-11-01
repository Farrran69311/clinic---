package clinic.ui;

import clinic.service.AuthService;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;

public class RegisterDialog extends JDialog {
    private final AuthService authService;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();

    public RegisterDialog(JFrame owner, AuthService authService) {
        super(owner, "注册患者账号", true);
        this.authService = authService;
        setSize(360, 240);
        setLocationRelativeTo(owner);
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("用户名"));
        form.add(usernameField);
        form.add(new JLabel("密码"));
        form.add(passwordField);
        form.add(new JLabel("确认密码"));
        form.add(confirmField);
        content.add(form, BorderLayout.CENTER);

        JButton registerButton = new JButton("注册");
        JButton cancelButton = new JButton("取消");
        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(cancelButton);
        content.add(buttons, BorderLayout.SOUTH);

        registerButton.addActionListener(e -> doRegister());
        cancelButton.addActionListener(e -> dispose());

        setContentPane(content);
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入完整信息", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "两次密码输入不一致", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "密码长度至少6位", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            authService.registerPatient(username, password);
            JOptionPane.showMessageDialog(this, "注册成功，请使用新账号登录", "成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "写入用户数据失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
