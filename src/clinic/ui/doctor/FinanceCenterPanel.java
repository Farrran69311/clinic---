package clinic.ui.doctor;

import clinic.AppContext;
import clinic.model.InsuranceClaim;
import clinic.model.Payment;
import clinic.model.User;
import clinic.service.InsuranceClaimService;
import clinic.service.PaymentService;
import clinic.ui.Refreshable;
import clinic.ui.common.TableUtils;
import clinic.ui.common.UIUtils;

import javax.swing.BorderFactory;
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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinanceCenterPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppContext context;
    private final User operator;
    private final PaymentService paymentService;
    private final InsuranceClaimService claimService;

    private final DefaultTableModel paymentModel;
    private final DefaultTableModel claimModel;
    private final JTable paymentTable;
    private final JTable claimTable;
    private final JLabel pendingAmountLabel = new JLabel("待支付：0.00");
    private final JLabel paidAmountLabel = new JLabel("已支付：0.00");
    private final JLabel refundAmountLabel = new JLabel("已退款：0.00");

    public FinanceCenterPanel(AppContext context, User operator) {
        this.context = context;
        this.operator = operator;
        this.paymentService = context.getPaymentService();
        this.claimService = context.getInsuranceClaimService();

        setLayout(new BorderLayout(10, 10));
        UIUtils.applyPagePadding(this);

        paymentModel = new DefaultTableModel(new String[]{
            "支付编号", "患者", "关联类型", "关联编号", "金额", "币种", "方式", "状态", "理赔编号", "创建时间", "完成时间"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        paymentTable = new JTable(paymentModel);
        paymentTable.setFillsViewportHeight(true);
        TableUtils.installRowPreview(paymentTable);

        claimModel = new DefaultTableModel(new String[]{
            "理赔编号", "支付编号", "类型", "报销比例", "申请金额", "核准金额", "状态", "提交时间", "处理时间", "备注"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        claimTable = new JTable(claimModel);
        claimTable.setFillsViewportHeight(true);
        TableUtils.installRowPreview(claimTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildPaymentPanel(), buildClaimPanel());
        splitPane.setResizeWeight(0.55);
        add(splitPane, BorderLayout.CENTER);

        add(buildSummaryPanel(), BorderLayout.SOUTH);
        refreshData();
    }

    private JPanel buildPaymentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("支付流水"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(paymentTable, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshPayments());
        header.add(refreshButton);
        panel.add(header, BorderLayout.NORTH);

        panel.add(new JScrollPane(paymentTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton("登记费用");
        JButton markPaidButton = new JButton("标记已支付");
        JButton markFailedButton = new JButton("标记失败");
        JButton refundButton = new JButton("退款");
        JButton submitClaimButton = new JButton("提交理赔");
        actions.add(createButton);
        actions.add(markPaidButton);
        actions.add(markFailedButton);
        actions.add(refundButton);
        actions.add(submitClaimButton);
        panel.add(actions, BorderLayout.SOUTH);

        createButton.addActionListener(e -> createPayment());
        markPaidButton.addActionListener(e -> markPaymentPaid());
        markFailedButton.addActionListener(e -> markPaymentFailed());
        refundButton.addActionListener(e -> refundPayment());
        submitClaimButton.addActionListener(e -> submitClaim());

        return panel;
    }

    private JPanel buildClaimPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UIUtils.applyHeaderSpacing(header);
        header.add(new JLabel("医保理赔"));
        header.add(new JLabel("搜索:"));
        JTextField searchField = new JTextField(18);
        TableUtils.installSearchFilter(claimTable, searchField);
        header.add(searchField);
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshClaims());
        header.add(refreshButton);
        panel.add(header, BorderLayout.NORTH);

        panel.add(new JScrollPane(claimTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approveButton = new JButton("审核通过");
        JButton rejectButton = new JButton("驳回理赔");
        JButton payoutButton = new JButton("完成打款");
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(payoutButton);
        panel.add(actions, BorderLayout.SOUTH);

        approveButton.addActionListener(e -> approveClaim());
        rejectButton.addActionListener(e -> rejectClaim());
        payoutButton.addActionListener(e -> completeClaimPayout());

        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summary.setBorder(BorderFactory.createTitledBorder("金额总览"));
        summary.add(pendingAmountLabel);
        summary.add(new JLabel(" | "));
        summary.add(paidAmountLabel);
        summary.add(new JLabel(" | "));
        summary.add(refundAmountLabel);
        return summary;
    }

    @Override
    public void refreshData() {
        refreshPayments();
        refreshClaims();
    }

    private void refreshPayments() {
        paymentModel.setRowCount(0);
        try {
            List<Payment> payments = paymentService.listAll();
            BigDecimal pending = paymentService.sumByStatus(payments, Payment.Status.PENDING)
                .add(paymentService.sumByStatus(payments, Payment.Status.PROCESSING));
            BigDecimal paid = paymentService.sumByStatus(payments, Payment.Status.PAID);
            BigDecimal refunded = paymentService.sumByStatus(payments, Payment.Status.REFUNDED);
            pendingAmountLabel.setText("待支付：" + pending.toPlainString());
            paidAmountLabel.setText("已支付：" + paid.toPlainString());
            refundAmountLabel.setText("已退款：" + refunded.toPlainString());
            for (Payment payment : payments) {
                paymentModel.addRow(new Object[]{
                    payment.getId(),
                    payment.getPatientId(),
                    payment.getRelatedType(),
                    payment.getRelatedId(),
                    payment.getAmount().toPlainString(),
                    payment.getCurrency(),
                    payment.getMethod(),
                    payment.getStatus(),
                    payment.getInsuranceClaimId(),
                    payment.getCreatedAt() == null ? "" : DATE_TIME_FORMATTER.format(payment.getCreatedAt()),
                    payment.getPaidAt() == null ? "" : DATE_TIME_FORMATTER.format(payment.getPaidAt())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载支付数据失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshClaims() {
        claimModel.setRowCount(0);
        try {
            List<InsuranceClaim> claims = claimService.listAll();
            for (InsuranceClaim claim : claims) {
                claimModel.addRow(new Object[]{
                    claim.getId(),
                    claim.getPaymentId(),
                    claim.getInsuranceType(),
                    claim.getCoverageRatio(),
                    claim.getClaimedAmount(),
                    claim.getApprovedAmount(),
                    claim.getStatus(),
                    claim.getSubmittedAt() == null ? "" : DATE_TIME_FORMATTER.format(claim.getSubmittedAt()),
                    claim.getProcessedAt() == null ? "" : DATE_TIME_FORMATTER.format(claim.getProcessedAt()),
                    claim.getNotes()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载理赔数据失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createPayment() {
        JTextField patientField = new JTextField();
        JComboBox<Payment.RelatedType> typeCombo = new JComboBox<>(Payment.RelatedType.values());
        JTextField relatedField = new JTextField();
        JTextField amountField = new JTextField("0.00");
        JTextField currencyField = new JTextField("CNY");
        JTextField methodField = new JTextField("CASH");
        JTextArea notesArea = new JTextArea(4, 18);
        notesArea.setLineWrap(true);
        Object[] message = {
            "患者编号", patientField,
            "关联类型", typeCombo,
            "关联编号", relatedField,
            "金额", amountField,
            "币种", currencyField,
            "支付方式", methodField,
            "备注（写入审计）", new JScrollPane(notesArea)
        };
        if (JOptionPane.showConfirmDialog(this, message, "登记费用", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            Payment payment = paymentService.createPayment(
                patientField.getText().trim(),
                (Payment.RelatedType) typeCombo.getSelectedItem(),
                relatedField.getText().trim().isEmpty() ? null : relatedField.getText().trim(),
                amount,
                currencyField.getText().trim().isEmpty() ? "CNY" : currencyField.getText().trim(),
                methodField.getText().trim().isEmpty() ? "CASH" : methodField.getText().trim()
            );
            logAudit("PAYMENT_CREATED", "PAYMENT", payment.getId(), notesArea.getText(), "SUCCESS");
            refreshPayments();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "金额格式不正确", "提示", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "创建支付失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markPaymentPaid() {
        String paymentId = getSelectedPaymentId();
        if (paymentId == null) {
            return;
        }
        try {
            paymentService.markPaid(paymentId);
            logAudit("PAYMENT_MARK_PAID", "PAYMENT", paymentId, "标记为已支付", "SUCCESS");
            refreshPayments();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "操作失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markPaymentFailed() {
        String paymentId = getSelectedPaymentId();
        if (paymentId == null) {
            return;
        }
        try {
            paymentService.markFailed(paymentId);
            logAudit("PAYMENT_MARK_FAILED", "PAYMENT", paymentId, "标记为失败", "SUCCESS");
            refreshPayments();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "操作失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refundPayment() {
        String paymentId = getSelectedPaymentId();
        if (paymentId == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确认对该支付执行退款？", "确认", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            paymentService.refund(paymentId);
            logAudit("PAYMENT_REFUND", "PAYMENT", paymentId, "执行退款", "SUCCESS");
            refreshPayments();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "退款失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitClaim() {
        String paymentId = getSelectedPaymentId();
        if (paymentId == null) {
            return;
        }
        JTextField typeField = new JTextField("PUBLIC");
        JTextField ratioField = new JTextField("0.8");
        JTextField amountField = new JTextField("0.00");
        JTextArea notesArea = new JTextArea(4, 18);
        notesArea.setLineWrap(true);
        Object[] message = {
            "医保类型", typeField,
            "覆盖比例 (0-1)", ratioField,
            "申请金额", amountField,
            "申请备注", new JScrollPane(notesArea)
        };
        if (JOptionPane.showConfirmDialog(this, message, "提交理赔", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            BigDecimal ratio = new BigDecimal(ratioField.getText().trim());
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            InsuranceClaim claim = claimService.submitClaim(paymentId, typeField.getText().trim(), ratio, amount, notesArea.getText());
            paymentService.attachInsuranceClaim(paymentId, claim.getId());
            logAudit("CLAIM_SUBMIT", "INSURANCE_CLAIM", claim.getId(), "提交理赔", "SUCCESS");
            refreshData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "比例或金额格式不正确", "提示", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "提交理赔失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveClaim() {
        String claimId = getSelectedClaimId();
        if (claimId == null) {
            return;
        }
        JTextField amountField = new JTextField("0.00");
        JTextArea notesArea = new JTextArea(4, 18);
        notesArea.setLineWrap(true);
        Object[] message = {
            "核准金额", amountField,
            "备注", new JScrollPane(notesArea)
        };
        if (JOptionPane.showConfirmDialog(this, message, "审核通过", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            claimService.approve(claimId, amount, notesArea.getText());
            logAudit("CLAIM_APPROVE", "INSURANCE_CLAIM", claimId, "审核通过", "SUCCESS");
            refreshClaims();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "金额格式不正确", "提示", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "审核失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectClaim() {
        String claimId = getSelectedClaimId();
        if (claimId == null) {
            return;
        }
        JTextArea notesArea = new JTextArea(4, 18);
        notesArea.setLineWrap(true);
        Object[] message = {
            "驳回原因", new JScrollPane(notesArea)
        };
        if (JOptionPane.showConfirmDialog(this, message, "驳回理赔", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            claimService.reject(claimId, notesArea.getText());
            logAudit("CLAIM_REJECT", "INSURANCE_CLAIM", claimId, notesArea.getText(), "SUCCESS");
            refreshClaims();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "驳回失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completeClaimPayout() {
        String claimId = getSelectedClaimId();
        if (claimId == null) {
            return;
        }
        try {
            claimService.completePayout(claimId);
            logAudit("CLAIM_PAYOUT", "INSURANCE_CLAIM", claimId, "完成打款", "SUCCESS");
            refreshClaims();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "打款失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getSelectedPaymentId() {
        int viewRow = paymentTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一笔支付记录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int modelRow = paymentTable.convertRowIndexToModel(viewRow);
        return paymentModel.getValueAt(modelRow, 0).toString();
    }

    private String getSelectedClaimId() {
        int viewRow = claimTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条理赔记录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int modelRow = claimTable.convertRowIndexToModel(viewRow);
        return claimModel.getValueAt(modelRow, 0).toString();
    }

    private void logAudit(String action, String entityType, String entityId, String detail, String result) {
        try {
            context.getAuditService().logAction(
                operator.getId(),
                operator.getRole().name(),
                action,
                entityType,
                entityId,
                detail == null ? "" : detail,
                result,
                null
            );
        } catch (Exception ignored) {
            // 审计失败不影响主流程
        }
    }
}
