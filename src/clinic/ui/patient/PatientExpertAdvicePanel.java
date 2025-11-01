package clinic.ui.patient;

import clinic.AppContext;
import clinic.model.ExpertAdvice;
import clinic.model.User;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;

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

public class PatientExpertAdvicePanel extends JPanel implements Refreshable {
    private final AppContext context;
    private final User user;
    private final DefaultTableModel model;

    public PatientExpertAdvicePanel(AppContext context, User user) {
        this.context = context;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        model = new DefaultTableModel(new String[]{"建议编号", "会诊编号", "建议日期", "建议概要", "随访计划"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("专家建议"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(table, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        add(header, BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                String summary = model.getValueAt(table.getSelectedRow(), 3).toString();
                String followUp = model.getValueAt(table.getSelectedRow(), 4).toString();
                JOptionPane.showMessageDialog(this,
                    "建议概要:\n" + summary + "\n\n随访计划:\n" + followUp,
                    "专家建议详情", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        refreshData();
    }

    @Override
    public void refreshData() {
        model.setRowCount(0);
        try {
            for (ExpertAdvice advice : context.getExpertAdviceService().listByPatient(user.getId())) {
                model.addRow(new Object[]{
                    advice.getId(),
                    advice.getSessionId(),
                    advice.getAdviceDate(),
                    advice.getAdviceSummary(),
                    advice.getFollowUpPlan()
                });
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "加载专家建议失败:" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
