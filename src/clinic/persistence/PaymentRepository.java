package clinic.persistence;

import clinic.model.Payment;
import clinic.model.Payment.RelatedType;
import clinic.model.Payment.Status;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PaymentRepository {
    private static final String HEADER = "id|patientId|relatedType|relatedId|amount|currency|method|status|insuranceClaimId|createdAt|paidAt";

    private final Path file;

    public PaymentRepository(Path file) {
        this.file = file;
    }

    public List<Payment> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Payment> payments = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 11) {
                continue;
            }
            payments.add(new Payment(
                row[0],
                row[1],
                parseRelatedType(row[2]),
                row[3].isEmpty() ? null : row[3],
                new BigDecimal(row[4]),
                row[5],
                row[6],
                parseStatus(row[7]),
                row[8].isEmpty() ? null : row[8],
                row[9].isEmpty() ? null : LocalDateTime.parse(row[9]),
                row[10].isEmpty() ? null : LocalDateTime.parse(row[10])
            ));
        }
        return payments;
    }

    public Optional<Payment> findById(String id) throws IOException {
        return findAll().stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public List<Payment> findByPatient(String patientId) throws IOException {
        return findAll().stream()
            .filter(p -> p.getPatientId().equals(patientId))
            .collect(Collectors.toList());
    }

    public void save(Payment payment) throws IOException {
        List<Payment> payments = findAll();
        boolean updated = false;
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getId().equals(payment.getId())) {
                payments.set(i, payment);
                updated = true;
                break;
            }
        }
        if (!updated) {
            payments.add(payment);
        }
        write(payments);
    }

    private void write(List<Payment> payments) throws IOException {
        List<String[]> rows = new ArrayList<>(payments.size());
        for (Payment payment : payments) {
            rows.add(new String[]{
                payment.getId(),
                payment.getPatientId(),
                payment.getRelatedType().name(),
                payment.getRelatedId() == null ? "" : payment.getRelatedId(),
                payment.getAmount().toPlainString(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getInsuranceClaimId() == null ? "" : payment.getInsuranceClaimId(),
                payment.getCreatedAt() == null ? "" : payment.getCreatedAt().toString(),
                payment.getPaidAt() == null ? "" : payment.getPaidAt().toString()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }

    private RelatedType parseRelatedType(String value) {
        try {
            return RelatedType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return RelatedType.OTHER;
        }
    }

    private Status parseStatus(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Status.PENDING;
        }
    }
}
