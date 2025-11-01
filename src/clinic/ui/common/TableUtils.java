package clinic.ui.common;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TableUtils {
    private TableUtils() {
    }

    public static void installSearchFilter(JTable table, JTextField field) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }

            private void apply() {
                String text = field.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                try {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                } catch (PatternSyntaxException ex) {
                    sorter.setRowFilter(null);
                }
            }
        });
    }

    public static void installRowPreview(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("clinic.rowPreview"))) {
            return;
        }
        ToolTipManager.sharedInstance().registerComponent(table);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    table.setToolTipText(null);
                    return;
                }
                table.setToolTipText(buildTooltip(table, viewRow));
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow >= 0) {
                        showDetailDialog(table, viewRow);
                    }
                }
            }
        });

        table.putClientProperty("clinic.rowPreview", Boolean.TRUE);
    }

    private static String buildTooltip(JTable table, int viewRow) {
        int modelRow = table.convertRowIndexToModel(viewRow);
        TableModel model = table.getModel();
        TableColumnModel columnModel = table.getColumnModel();
        int columns = Math.min(columnModel.getColumnCount(), 4);
        StringBuilder builder = new StringBuilder("<html>");
        for (int viewCol = 0; viewCol < columns; viewCol++) {
            int modelCol = table.convertColumnIndexToModel(viewCol);
            String header = String.valueOf(columnModel.getColumn(viewCol).getHeaderValue());
            Object value = model.getValueAt(modelRow, modelCol);
            builder.append(escape(String.valueOf(header))).append("：")
                .append(escape(value == null ? "" : value.toString()));
            if (viewCol < columns - 1) {
                builder.append("<br/>");
            }
        }
        builder.append("</html>");
        return builder.toString();
    }

    private static void showDetailDialog(JTable table, int viewRow) {
        int modelRow = table.convertRowIndexToModel(viewRow);
        TableModel model = table.getModel();
        TableColumnModel columnModel = table.getColumnModel();
        StringBuilder detail = new StringBuilder();
        for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
            int modelCol = table.convertColumnIndexToModel(viewCol);
            String header = String.valueOf(columnModel.getColumn(viewCol).getHeaderValue());
            Object value = model.getValueAt(modelRow, modelCol);
            detail.append(header).append("：").append(value == null ? "" : value.toString());
            if (viewCol < columnModel.getColumnCount() - 1) {
                detail.append('\n');
            }
        }
        JTextArea area = new JTextArea(detail.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(420, 260));
        JOptionPane.showMessageDialog(table, scrollPane, "详细信息", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
