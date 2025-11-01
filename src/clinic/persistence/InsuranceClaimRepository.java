package clinic.persistence;

import clinic.model.InsuranceClaim;
import clinic.model.InsuranceClaim.Status;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InsuranceClaimRepository {
    private static final String HEADER = "id|paymentId|insuranceType|coverageRatio|claimedAmount|approvedAmount|status|submittedAt|processedAt|notes";

    private final Path file;

    public InsuranceClaimRepository(Path file) {
        this.file = file;
    }

    public List<InsuranceClaim> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<InsuranceClaim> claims = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 10) {
                continue;
            }
            claims.add(new InsuranceClaim(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? null : new BigDecimal(row[3]),
                row[4].isEmpty() ? null : new BigDecimal(row[4]),
                row[5].isEmpty() ? null : new BigDecimal(row[5]),
                parseStatus(row[6]),
                row[7].isEmpty() ? null : LocalDateTime.parse(row[7]),
                row[8].isEmpty() ? null : LocalDateTime.parse(row[8]),
                row[9]
            ));
        }
        return claims;
    }

    public Optional<InsuranceClaim> findById(String id) throws IOException {
        return findAll().stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public List<InsuranceClaim> findByPaymentId(String paymentId) throws IOException {
        return findAll().stream()
            .filter(c -> c.getPaymentId().equals(paymentId))
            .collect(Collectors.toList());
    }

    public void save(InsuranceClaim claim) throws IOException {
        List<InsuranceClaim> claims = findAll();
        boolean updated = false;
        for (int i = 0; i < claims.size(); i++) {
            if (claims.get(i).getId().equals(claim.getId())) {
                claims.set(i, claim);
                updated = true;
                break;
            }
        }
        if (!updated) {
            claims.add(claim);
        }
        write(claims);
    }

    private void write(List<InsuranceClaim> claims) throws IOException {
        List<String[]> rows = new ArrayList<>(claims.size());
        for (InsuranceClaim claim : claims) {
            rows.add(new String[]{
                claim.getId(),
                claim.getPaymentId(),
                claim.getInsuranceType(),
                claim.getCoverageRatio().toPlainString(),
                claim.getClaimedAmount().toPlainString(),
                claim.getApprovedAmount() == null ? "" : claim.getApprovedAmount().toPlainString(),
                claim.getStatus().name(),
                claim.getSubmittedAt() == null ? "" : claim.getSubmittedAt().toString(),
                claim.getProcessedAt() == null ? "" : claim.getProcessedAt().toString(),
                claim.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }

    private Status parseStatus(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Status.PENDING;
        }
    }
}
