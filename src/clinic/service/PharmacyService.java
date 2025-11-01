package clinic.service;

import clinic.model.Medicine;
import clinic.model.Prescription;
import clinic.persistence.CsvDataStore;
import clinic.persistence.MedicineRepository;
import clinic.persistence.PrescriptionRepository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PharmacyService {
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    public PharmacyService(MedicineRepository medicineRepository, PrescriptionRepository prescriptionRepository) {
        this.medicineRepository = medicineRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    public List<Medicine> listMedicines() throws IOException {
        return medicineRepository.findAll();
    }

    public List<Prescription> listPrescriptions() throws IOException {
        return prescriptionRepository.findAll();
    }

    public Medicine addMedicine(String name, String specification, int stock, String unit, java.time.LocalDate expiryDate) throws IOException {
        Medicine medicine = new Medicine(
            CsvDataStore.randomId(),
            name,
            specification,
            stock,
            unit,
            expiryDate
        );
        medicineRepository.save(medicine);
        return medicine;
    }

    public void updateMedicine(Medicine medicine) throws IOException {
        medicineRepository.save(medicine);
    }

    public void removeMedicine(String id) throws IOException {
        medicineRepository.deleteById(id);
    }

    public Prescription createPrescription(String consultationId, String medicineId, int quantity, String usage) throws IOException {
        Prescription prescription = new Prescription(
            CsvDataStore.randomId(),
            consultationId,
            medicineId,
            quantity,
            usage,
            "PENDING"
        );
        prescriptionRepository.save(prescription);
        return prescription;
    }

    public void updatePrescriptionStatus(String prescriptionId, String status) throws IOException {
        List<Prescription> prescriptions = prescriptionRepository.findAll();
        for (Prescription prescription : prescriptions) {
            if (prescription.getId().equals(prescriptionId)) {
                Prescription updated = new Prescription(
                    prescription.getId(),
                    prescription.getConsultationId(),
                    prescription.getMedicineId(),
                    prescription.getQuantity(),
                    prescription.getUsage(),
                    status
                );
                prescriptionRepository.save(updated);
                return;
            }
        }
        throw new IllegalArgumentException("未找到处方");
    }

    public List<Prescription> listPendingPrescriptions() throws IOException {
        return prescriptionRepository.findAll().stream()
            .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
            .collect(Collectors.toList());
    }
}
