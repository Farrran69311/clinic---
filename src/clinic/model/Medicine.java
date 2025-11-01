package clinic.model;

import java.time.LocalDate;
import java.util.Objects;

public class Medicine {
    private final String id;
    private final String name;
    private final String specification;
    private final int stock;
    private final String unit;
    private final LocalDate expiryDate;

    public Medicine(String id, String name, String specification, int stock, String unit, LocalDate expiryDate) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.specification = specification;
        this.stock = Math.max(stock, 0);
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecification() {
        return specification;
    }

    public int getStock() {
        return stock;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
