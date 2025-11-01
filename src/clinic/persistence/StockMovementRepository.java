package clinic.persistence;

import clinic.model.StockMovement;
import clinic.model.StockMovement.MovementType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StockMovementRepository {
    private static final String HEADER = "id|medicineId|movementType|quantity|unitCost|totalCost|occurredAt|referenceType|referenceId|operatorId|notes";

    private final Path file;

    public StockMovementRepository(Path file) {
        this.file = file;
    }

    public List<StockMovement> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<StockMovement> movements = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 11) {
                continue;
            }
            movements.add(new StockMovement(
                row[0],
                row[1],
                parseType(row[2]),
                row[3].isEmpty() ? 0 : Integer.parseInt(row[3]),
                row[4].isEmpty() ? null : new BigDecimal(row[4]),
                row[5].isEmpty() ? null : new BigDecimal(row[5]),
                row[6].isEmpty() ? null : LocalDateTime.parse(row[6]),
                row[7].isEmpty() ? null : row[7],
                row[8].isEmpty() ? null : row[8],
                row[9].isEmpty() ? null : row[9],
                row[10]
            ));
        }
        return movements;
    }

    public List<StockMovement> findByMedicine(String medicineId) throws IOException {
        return findAll().stream()
            .filter(m -> m.getMedicineId().equals(medicineId))
            .collect(Collectors.toList());
    }

    public void save(StockMovement movement) throws IOException {
        List<StockMovement> movements = findAll();
        movements.add(movement);
        write(movements);
    }

    private void write(List<StockMovement> movements) throws IOException {
        List<String[]> rows = new ArrayList<>(movements.size());
        for (StockMovement movement : movements) {
            rows.add(new String[]{
                movement.getId(),
                movement.getMedicineId(),
                movement.getMovementType().name(),
                Integer.toString(movement.getQuantity()),
                movement.getUnitCost().toPlainString(),
                movement.getTotalCost().toPlainString(),
                movement.getOccurredAt().toString(),
                movement.getReferenceType() == null ? "" : movement.getReferenceType(),
                movement.getReferenceId() == null ? "" : movement.getReferenceId(),
                movement.getOperatorId() == null ? "" : movement.getOperatorId(),
                movement.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }

    private MovementType parseType(String value) {
        try {
            return MovementType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return MovementType.ADJUSTMENT;
        }
    }
}
