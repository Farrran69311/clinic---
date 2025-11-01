package clinic.ui.administrator;

import clinic.AppContext;
import clinic.model.AuditLog;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;
import clinic.ui.common.UIUtils;

import javax.swing.BorderFactory;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class AuditLogPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppContext context;
    private final DefaultTableModel tableModel;
    private final JTable auditTable;

    public AuditLogPanel(AppContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);

        tableModel = new DefaultTableModel(new String[]{
            "编号", "用户", "角色", "动作", "对象类型", "对象编号", "结果", "时间", "详情"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        auditTable = new JTable(tableModel);
        auditTable.setFillsViewportHeight(true);
        TableUtils.installRowPreview(auditTable);

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(auditTable), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refreshData();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("审计日志"));
        header.add(new JLabel("搜索:"));
        JTextField filterField = new JTextField(18);
        TableUtils.installSearchFilter(auditTable, filterField);
        header.add(filterField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBorder(BorderFactory.createTitledBorder("说明"));
        footer.add(new JLabel("系统会记录关键操作，包括支付、库存调整、理赔等，便于后续追踪审计。"));
        return footer;
    }

    @Override
    public void refreshData() {
        tableModel.setRowCount(0);
        try {
            context.getAuditService().listAll().stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .forEach(log -> tableModel.addRow(new Object[]{
                    log.getId(),
                    log.getUserId(),
                    log.getRole(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getResult(),
                    log.getTimestamp() == null ? "" : DATE_TIME_FORMATTER.format(log.getTimestamp()),
                    log.getDetail()
                }));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载审计日志失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
