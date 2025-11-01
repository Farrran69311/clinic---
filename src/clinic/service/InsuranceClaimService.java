package clinic.service;

import clinic.model.InsuranceClaim;
import clinic.model.InsuranceClaim.Status;
import clinic.persistence.CsvDataStore;
import clinic.persistence.InsuranceClaimRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class InsuranceClaimService {
    private final InsuranceClaimRepository claimRepository;

    public InsuranceClaimService(InsuranceClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public List<InsuranceClaim> listAll() throws IOException {
        return claimRepository.findAll();
    }

    public Optional<InsuranceClaim> findById(String id) throws IOException {
        return claimRepository.findById(id);
    }

    public List<InsuranceClaim> findByPaymentId(String paymentId) throws IOException {
        return claimRepository.findByPaymentId(paymentId);
    }

    public InsuranceClaim submitClaim(String paymentId,
                                      String insuranceType,
                                      BigDecimal coverageRatio,
                                      BigDecimal claimedAmount,
                                      String notes) throws IOException {
        InsuranceClaim claim = new InsuranceClaim(
            CsvDataStore.randomId(),
            paymentId,
            insuranceType,
            coverageRatio,
            claimedAmount,
            null,
            Status.SUBMITTED,
            LocalDateTime.now(),
            null,
            notes
        );
        claimRepository.save(claim);
        return claim;
    }

    public InsuranceClaim approve(String claimId, BigDecimal approvedAmount, String notes) throws IOException {
        InsuranceClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new IllegalArgumentException("未找到医保理赔"));
        InsuranceClaim updated = claim.withStatus(Status.APPROVED, approvedAmount, LocalDateTime.now(), notes);
        claimRepository.save(updated);
        return updated;
    }

    public InsuranceClaim reject(String claimId, String notes) throws IOException {
        InsuranceClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new IllegalArgumentException("未找到医保理赔"));
        InsuranceClaim updated = claim.withStatus(Status.REJECTED, BigDecimal.ZERO, LocalDateTime.now(), notes);
        claimRepository.save(updated);
        return updated;
    }

    public InsuranceClaim completePayout(String claimId) throws IOException {
        InsuranceClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new IllegalArgumentException("未找到医保理赔"));
        InsuranceClaim updated = claim.withStatus(Status.PAID, claim.getApprovedAmount(), LocalDateTime.now(), claim.getNotes());
        claimRepository.save(updated);
        return updated;
    }
}
