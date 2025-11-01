package clinic.persistence;

import clinic.model.Medicine;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicineRepository {
    private static final String HEADER = "id|name|specification|stock|unit|expiryDate";

    private final Path file;

    public MedicineRepository(Path file) {
        this.file = file;
    }

    public List<Medicine> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Medicine> medicines = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 6) {
                continue;
            }
            medicines.add(new Medicine(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? 0 : Integer.parseInt(row[3]),
                row[4],
                row[5].isEmpty() ? null : LocalDate.parse(row[5])
            ));
        }
        return medicines;
    }

    public void save(Medicine medicine) throws IOException {
        List<Medicine> medicines = findAll();
        boolean updated = false;
        for (int i = 0; i < medicines.size(); i++) {
            if (medicines.get(i).getId().equals(medicine.getId())) {
                medicines.set(i, medicine);
                updated = true;
                break;
            }
        }
        if (!updated) {
            medicines.add(medicine);
        }
        write(medicines);
    }

    public void deleteById(String id) throws IOException {
        List<Medicine> medicines = findAll();
        medicines.removeIf(m -> m.getId().equals(id));
        write(medicines);
    }

    private void write(List<Medicine> medicines) throws IOException {
        List<String[]> rows = new ArrayList<>(medicines.size());
        for (Medicine medicine : medicines) {
            rows.add(new String[]{
                medicine.getId(),
                medicine.getName(),
                medicine.getSpecification() == null ? "" : medicine.getSpecification(),
                Integer.toString(medicine.getStock()),
                medicine.getUnit() == null ? "" : medicine.getUnit(),
                medicine.getExpiryDate() == null ? "" : medicine.getExpiryDate().toString()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
