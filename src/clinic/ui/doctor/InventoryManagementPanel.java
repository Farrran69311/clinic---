package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.Medicine;
import clinic.model.StockMovement;
import clinic.model.User;
import clinic.service.InventoryService;
import clinic.service.PharmacyService;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;
import clinic.ui.common.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryManagementPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppContext context;
    private final User operator;
    private final InventoryService inventoryService;
    private final PharmacyService pharmacyService;

    private final DefaultTableModel movementModel;
    private final JTable movementTable;
    private final Map<String, StockMovement> movementCache = new HashMap<>();
    private final JLabel onHandLabel = new JLabel("当前库存：0");
    private final JLabel inventoryValueLabel = new JLabel("库存金额：0.00");

    public InventoryManagementPanel(AppContext context, User operator) {
        this.context = context;
        this.operator = operator;
        this.inventoryService = context.getInventoryService();
        this.pharmacyService = context.getPharmacyService();

        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);

        movementModel = new DefaultTableModel(new String[]{
            "流水号", "药品", "类型", "数量", "单位成本", "来源类型", "来源编号", "备注", "操作人", "时间"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movementTable = new JTable(movementModel);
        movementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movementTable.setFillsViewportHeight(true);
        TableUtils.installRowPreview(movementTable);
        movementTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateMedicineSummary();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildMovementPanel(), buildSummaryPanel());
        splitPane.setResizeWeight(0.8);
        add(splitPane, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildMovementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("库存流水"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(16);
        TableUtils.installSearchFilter(movementTable, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        header.add(refreshButton);
        panel.add(header, BorderLayout.NORTH);

        panel.add(new JScrollPane(movementTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton inboundButton = new JButton("入库");
        JButton outboundButton = new JButton("出库");
        JButton adjustButton = new JButton("盘点调整");
        actions.add(inboundButton);
        actions.add(outboundButton);
        actions.add(adjustButton);
        panel.add(actions, BorderLayout.SOUTH);

        inboundButton.addActionListener(e -> recordMovement(StockMovement.MovementType.INBOUND));
        outboundButton.addActionListener(e -> recordMovement(StockMovement.MovementType.OUTBOUND));
        adjustButton.addActionListener(e -> adjustInventory());

        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("药品库存概览"));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.add(onHandLabel);
        statsPanel.add(new JLabel(" | "));
        statsPanel.add(inventoryValueLabel);

        panel.add(statsPanel, BorderLayout.NORTH);

        JTextArea tips = new JTextArea("提示：选择列表中的药品后，可查看其当前库存和金额。通过入库、出库或盘点调整保持库存准确。");
        tips.setWrapStyleWord(true);
        tips.setLineWrap(true);
        tips.setEditable(false);
        tips.setBackground(getBackground());
        panel.add(tips, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refreshData() {
        movementModel.setRowCount(0);
        try {
            movementCache.clear();
            Map<String, Medicine> medicineMap = pharmacyService.listMedicines().stream()
                .collect(Collectors.toMap(Medicine::getId, m -> m));
            List<StockMovement> movements = inventoryService.listAll().stream()
                .sorted(Comparator.comparing(StockMovement::getOccurredAt).reversed())
                .collect(Collectors.toList());
            for (StockMovement movement : movements) {
                movementCache.put(movement.getId(), movement);
                Medicine medicine = medicineMap.get(movement.getMedicineId());
                movementModel.addRow(new Object[]{
                    movement.getId(),
                    medicine == null ? movement.getMedicineId() : medicine.getName(),
                    movement.getMovementType(),
                    movement.getQuantity(),
                    movement.getUnitCost(),
                    movement.getReferenceType(),
                    movement.getReferenceId(),
                    movement.getNotes(),
                    movement.getOperatorId(),
                    movement.getOccurredAt() == null ? "" : DATE_TIME_FORMATTER.format(movement.getOccurredAt())
                });
            }
            updateMedicineSummary();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载库存数据失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordMovement(StockMovement.MovementType type) {
        MovementForm form = new MovementForm(type);
        if (!form.showDialog()) {
            return;
        }
        try {
            StockMovement movement;
            switch (type) {
                case INBOUND:
                    movement = inventoryService.recordInbound(
                        form.medicineId,
                        form.quantity,
                        form.unitCost,
                        form.referenceType,
                        form.referenceId,
                        operator.getId(),
                        form.notes
                    );
                    break;
                case OUTBOUND:
                    movement = inventoryService.recordOutbound(
                        form.medicineId,
                        form.quantity,
                        form.unitCost,
                        form.referenceType,
                        form.referenceId,
                        operator.getId(),
                        form.notes
                    );
                    break;
                default:
                    throw new IllegalStateException("Unsupported movement type" + type);
            }
            logAudit("INVENTORY_" + type.name(), movement);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "记录库存失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adjustInventory() {
        MovementForm form = new MovementForm(StockMovement.MovementType.ADJUSTMENT);
        if (!form.showDialog()) {
            return;
        }
        try {
            StockMovement movement = inventoryService.recordAdjustment(
                form.medicineId,
                form.quantity,
                form.unitCost,
                form.notes,
                operator.getId()
            );
            logAudit("INVENTORY_ADJUST", movement);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "盘点调整失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMedicineSummary() {
        int viewRow = movementTable.getSelectedRow();
        if (viewRow < 0) {
            onHandLabel.setText("当前库存：0");
            inventoryValueLabel.setText("库存金额：0.00");
            return;
        }
        int modelRow = movementTable.convertRowIndexToModel(viewRow);
        Object medicineNameObj = movementModel.getValueAt(modelRow, 1);
        String medicineName = medicineNameObj == null ? "" : medicineNameObj.toString();
        String movementId = movementModel.getValueAt(modelRow, 0).toString();
        StockMovement movement = movementCache.get(movementId);
        if (movement == null) {
            return;
        }
        try {
            int onHand = inventoryService.calculateOnHandQuantity(movement.getMedicineId());
            BigDecimal totalValue = inventoryService.calculateInventoryValue(movement.getMedicineId());
            onHandLabel.setText("当前库存(" + medicineName + ")：" + onHand);
            inventoryValueLabel.setText("库存金额：" + totalValue.stripTrailingZeros().toPlainString());
        } catch (Exception ex) {
            onHandLabel.setText("当前库存：错误");
            inventoryValueLabel.setText("库存金额：错误");
        }
    }

    private void logAudit(String action, StockMovement movement) {
        try {
            context.getAuditService().logAction(
                operator.getId(),
                operator.getRole().name(),
                action,
                "STOCK_MOVEMENT",
                movement.getId(),
                buildAuditDetail(movement),
                "SUCCESS",
                null
            );
        } catch (Exception ignored) {
            // 忽略审计写入失败
        }
    }

    private String buildAuditDetail(StockMovement movement) {
        return "药品=" + movement.getMedicineId() +
            ", 类型=" + movement.getMovementType() +
            ", 数量=" + movement.getQuantity() +
            ", 备注=" + (movement.getNotes() == null ? "" : movement.getNotes());
    }

    private class MovementForm {
        private final StockMovement.MovementType type;
        private String medicineId;
    private int quantity;
    private BigDecimal unitCost = BigDecimal.ZERO;
        private String referenceType;
        private String referenceId;
        private String notes;

        MovementForm(StockMovement.MovementType type) {
            this.type = type;
        }

        boolean showDialog() {
            try {
                List<Medicine> medicines = pharmacyService.listMedicines();
                if (medicines.isEmpty()) {
                    JOptionPane.showMessageDialog(InventoryManagementPanel.this, "尚未维护药品目录", "提示", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                JComboBox<Medicine> medicineCombo = new JComboBox<>(medicines.toArray(new Medicine[0]));
                medicineCombo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof Medicine) {
                            Medicine medicine = (Medicine) value;
                            label.setText(medicine.getName() + " (" + medicine.getId() + ")");
                        }
                        return label;
                    }
                });
                JTextField quantityField = new JTextField(
                    type == StockMovement.MovementType.INBOUND || type == StockMovement.MovementType.OUTBOUND ? "1" : "0"
                );
                JTextField costField = new JTextField("0.00");
                JTextField refTypeField = new JTextField("PURCHASE_ORDER");
                JTextField refIdField = new JTextField();
                JTextArea noteArea = new JTextArea(4, 18);
                noteArea.setLineWrap(true);
                Object[] message = buildFormMessage(medicineCombo, quantityField, costField, refTypeField, refIdField, noteArea);
                if (JOptionPane.showConfirmDialog(
                    InventoryManagementPanel.this,
                    message,
                    type == StockMovement.MovementType.ADJUSTMENT ? "盘点调整" : (type == StockMovement.MovementType.INBOUND ? "药品入库" : "药品出库"),
                    JOptionPane.OK_CANCEL_OPTION
                ) != JOptionPane.OK_OPTION) {
                    return false;
                }
                Medicine selectedMedicine = (Medicine) medicineCombo.getSelectedItem();
                if (selectedMedicine == null) {
                    throw new IllegalStateException("请选择药品");
                }
                medicineId = selectedMedicine.getId();
                quantity = Integer.parseInt(quantityField.getText().trim());
                if (type != StockMovement.MovementType.ADJUSTMENT && quantity <= 0) {
                    throw new IllegalArgumentException("数量必须大于0");
                }
                unitCost = new BigDecimal(costField.getText().trim());
                referenceType = refTypeField.getText().trim();
                referenceId = refIdField.getText().trim();
                notes = noteArea.getText();
                return true;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(InventoryManagementPanel.this, "数量或成本格式不正确", "提示", JOptionPane.WARNING_MESSAGE);
                return false;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(InventoryManagementPanel.this, "表单输入错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        private Object[] buildFormMessage(JComboBox<Medicine> medicineCombo,
                                           JTextField quantityField,
                                           JTextField costField,
                                           JTextField refTypeField,
                                           JTextField refIdField,
                                           JTextArea noteArea) {
            if (type == StockMovement.MovementType.ADJUSTMENT) {
                return new Object[]{
                    "药品", medicineCombo,
                    "数量变化(可为负)", quantityField,
                    "单位成本(可选)", costField,
                    "备注", new JScrollPane(noteArea)
                };
            }
            return new Object[]{
                "药品", medicineCombo,
                "数量", quantityField,
                "单位成本", costField,
                "来源类型", refTypeField,
                "来源编号", refIdField,
                "备注", new JScrollPane(noteArea)
            };
        }
    }
}
