package clinic.service;

import clinic.model.StockMovement;
import clinic.model.StockMovement.MovementType;
import clinic.persistence.CsvDataStore;
import clinic.persistence.StockMovementRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InventoryService {
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<StockMovement> listAll() throws IOException {
        return stockMovementRepository.findAll();
    }

    public List<StockMovement> listByMedicine(String medicineId) throws IOException {
        return stockMovementRepository.findByMedicine(medicineId);
    }

    public StockMovement recordInbound(String medicineId,
                                       int quantity,
                                       BigDecimal unitCost,
                                       String referenceType,
                                       String referenceId,
                                       String operatorId,
                                       String notes) throws IOException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量必须大于0");
        }
        StockMovement movement = new StockMovement(
            CsvDataStore.randomId(),
            medicineId,
            MovementType.INBOUND,
            quantity,
            unitCost,
            null,
            LocalDateTime.now(),
            referenceType,
            referenceId,
            operatorId,
            notes
        );
        stockMovementRepository.save(movement);
        return movement;
    }

    public StockMovement recordOutbound(String medicineId,
                                        int quantity,
                                        BigDecimal unitCost,
                                        String referenceType,
                                        String referenceId,
                                        String operatorId,
                                        String notes) throws IOException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("出库数量必须大于0");
        }
        StockMovement movement = new StockMovement(
            CsvDataStore.randomId(),
            medicineId,
            MovementType.OUTBOUND,
            -Math.abs(quantity),
            unitCost,
            null,
            LocalDateTime.now(),
            referenceType,
            referenceId,
            operatorId,
            notes
        );
        stockMovementRepository.save(movement);
        return movement;
    }

    public StockMovement recordAdjustment(String medicineId,
                                          int quantityChange,
                                          BigDecimal unitCost,
                                          String reason,
                                          String operatorId) throws IOException {
        StockMovement movement = new StockMovement(
            CsvDataStore.randomId(),
            medicineId,
            MovementType.ADJUSTMENT,
            quantityChange,
            unitCost,
            null,
            LocalDateTime.now(),
            "ADJUSTMENT",
            null,
            operatorId,
            reason
        );
        stockMovementRepository.save(movement);
        return movement;
    }

    public BigDecimal calculateInventoryValue(String medicineId) throws IOException {
        BigDecimal total = BigDecimal.ZERO;
        for (StockMovement movement : stockMovementRepository.findByMedicine(medicineId)) {
            total = total.add(movement.getTotalCost());
        }
        return total;
    }

    public int calculateOnHandQuantity(String medicineId) throws IOException {
        int total = 0;
        for (StockMovement movement : stockMovementRepository.findByMedicine(medicineId)) {
            total += movement.getQuantity();
        }
        return total;
    }
}
