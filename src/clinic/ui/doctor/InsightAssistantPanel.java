package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Doctor;
import clinic.model.Patient;
import clinic.service.InsightService;
import clinic.ui.Refreshable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class InsightAssistantPanel extends JPanel implements Refreshable {
    private static final String PLACEHOLDER = "请选择患者或医生后生成总结。";
    private final AppContext context;
    private final InsightService insightService;
    private final DefaultComboBoxModel<Item> patientModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<Item> doctorModel = new DefaultComboBoxModel<>();
    private final JComboBox<Item> patientCombo = new JComboBox<>(patientModel);
    private final JComboBox<Item> doctorCombo = new JComboBox<>(doctorModel);
    private final JTextArea outputArea = new JTextArea();

    public InsightAssistantPanel(AppContext context) {
        this.context = context;
        this.insightService = context.getInsightService();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildResultArea(), BorderLayout.CENTER);
        add(new JLabel("说明：此功能基于现有记录进行规则化分析，供参考使用。"), BorderLayout.SOUTH);

        refreshData();
    }

    private JPanel buildControlPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createTitledBorder("智能助手"));

        JPanel patientRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        patientRow.add(new JLabel("选择患者:"));
        patientCombo.setPrototypeDisplayValue(new Item("", "示例患者名称"));
        patientRow.add(patientCombo);
        JButton patientButton = new JButton("生成患者总结");
        patientButton.addActionListener(e -> generatePatientInsight());
        patientRow.add(patientButton);
        wrapper.add(patientRow);

        JPanel doctorRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        doctorRow.add(new JLabel("选择医生:"));
        doctorCombo.setPrototypeDisplayValue(new Item("", "示例医生名称"));
        doctorRow.add(doctorCombo);
        JButton doctorButton = new JButton("生成周度报告");
        doctorButton.addActionListener(e -> generateDoctorSummary());
        doctorRow.add(doctorButton);
        wrapper.add(doctorRow);

        JPanel refreshRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton refreshButton = new JButton("刷新名单");
        refreshButton.addActionListener(e -> refreshData());
        refreshRow.add(refreshButton);
        wrapper.add(refreshRow);

        return wrapper;
    }

    private JScrollPane buildResultArea() {
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(outputArea.getFont().deriveFont(14f));
        return new JScrollPane(outputArea);
    }

    @Override
    public void refreshData() {
        String previous = outputArea.getText();
        boolean keepContent = previous != null && !previous.isBlank() && !PLACEHOLDER.equals(previous.trim());
        try {
            loadPatients();
            loadDoctors();
            if (!keepContent) {
                outputArea.setText(PLACEHOLDER);
            }
        } catch (IOException ex) {
            showError("刷新数据失败", ex);
        }
    }

    private void loadPatients() throws IOException {
        String selectedId = null;
        Item current = (Item) patientCombo.getSelectedItem();
        if (current != null) {
            selectedId = current.id();
        }
        patientModel.removeAllElements();
        List<Patient> patients = context.getPatientService().listPatients();
        Item toSelect = null;
        for (Patient patient : patients) {
            Item item = new Item(patient.getId(), patient.getName());
            patientModel.addElement(item);
            if (patient.getId().equals(selectedId)) {
                toSelect = item;
            }
        }
        if (toSelect != null) {
            patientCombo.setSelectedItem(toSelect);
        } else if (patientModel.getSize() > 0) {
            patientCombo.setSelectedIndex(0);
        }
    }

    private void loadDoctors() throws IOException {
        String selectedId = null;
        Item current = (Item) doctorCombo.getSelectedItem();
        if (current != null) {
            selectedId = current.id();
        }
        doctorModel.removeAllElements();
        List<Doctor> doctors = context.getDoctorService().listDoctors();
        Item toSelect = null;
        for (Doctor doctor : doctors) {
            Item item = new Item(doctor.getId(), doctor.getName());
            doctorModel.addElement(item);
            if (doctor.getId().equals(selectedId)) {
                toSelect = item;
            }
        }
        if (toSelect != null) {
            doctorCombo.setSelectedItem(toSelect);
        } else if (doctorModel.getSize() > 0) {
            doctorCombo.setSelectedIndex(0);
        }
    }

    private void generatePatientInsight() {
        Item selected = (Item) patientCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择患者", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String insight = insightService.buildPatientInsight(selected.id());
            outputArea.setText(insight);
            outputArea.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            showError("生成患者总结失败", ex);
        }
    }

    private void generateDoctorSummary() {
        Item selected = (Item) doctorCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择医生", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String summary = insightService.buildDoctorWeeklySummary(selected.id(), LocalDate.now());
            outputArea.setText(summary);
            outputArea.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            showError("生成周度报告失败", ex);
        }
    }

    private void showError(String message, IOException ex) {
        JOptionPane.showMessageDialog(this, message + ": " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }

    private static class Item {
        private final String id;
        private final String label;

        Item(String id, String label) {
            this.id = id;
            this.label = label;
        }

        String id() {
            return id;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
