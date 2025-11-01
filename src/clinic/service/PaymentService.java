package clinic.service;

import clinic.model.Payment;
import clinic.model.Payment.RelatedType;
import clinic.model.Payment.Status;
import clinic.persistence.CsvDataStore;
import clinic.persistence.PaymentRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> listAll() throws IOException {
        return paymentRepository.findAll();
    }

    public List<Payment> listByPatient(String patientId) throws IOException {
        return paymentRepository.findByPatient(patientId);
    }

    public Optional<Payment> findById(String id) throws IOException {
        return paymentRepository.findById(id);
    }

    public Payment createPayment(String patientId,
                                 RelatedType relatedType,
                                 String relatedId,
                                 BigDecimal amount,
                                 String currency,
                                 String method) throws IOException {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("支付金额不能为负");
        }
        Payment payment = new Payment(
            CsvDataStore.randomId(),
            patientId,
            relatedType,
            relatedId,
            amount,
            currency,
            method,
            Status.PENDING,
            null,
            LocalDateTime.now(),
            null
        );
        paymentRepository.save(payment);
        return payment;
    }

    public Payment markPaid(String paymentId) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("未找到支付记录"));
        Payment updated = payment.withStatus(Status.PAID, LocalDateTime.now());
        paymentRepository.save(updated);
        return updated;
    }

    public Payment markFailed(String paymentId) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("未找到支付记录"));
        Payment updated = payment.withStatus(Status.FAILED, payment.getPaidAt());
        paymentRepository.save(updated);
        return updated;
    }

    public Payment refund(String paymentId) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("未找到支付记录"));
        if (payment.getStatus() != Status.PAID) {
            throw new IllegalStateException("非已支付订单无法退款");
        }
        Payment updated = payment.withStatus(Status.REFUNDED, LocalDateTime.now());
        paymentRepository.save(updated);
        return updated;
    }

    public BigDecimal sumByStatus(List<Payment> payments, Status status) {
        return payments.stream()
            .filter(p -> p.getStatus() == status)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateRevenue(LocalDateTime from, LocalDateTime to) throws IOException {
        return paymentRepository.findAll().stream()
            .filter(p -> p.getStatus() == Status.PAID)
            .filter(p -> !p.getPaidAt().isBefore(from) && !p.getPaidAt().isAfter(to))
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> listOutstanding() throws IOException {
        return paymentRepository.findAll().stream()
            .filter(p -> p.getStatus() == Status.PENDING || p.getStatus() == Status.PROCESSING)
            .collect(Collectors.toList());
    }

    public void attachInsuranceClaim(String paymentId, String claimId) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("未找到支付记录"));
        paymentRepository.save(payment.withInsuranceClaim(claimId));
    }
}
