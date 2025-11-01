package clinic.model;

import java.util.Objects;

public class Doctor {
    private final String id;
    private final String name;
    private final String department;
    private final String phone;
    private final String schedule;
    private final Double rating;
    private final String title;
    private final String level;
    private final String specialties;

    public Doctor(String id, String name, String department, String phone, String schedule, Double rating, String title, String level, String specialties) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.department = department;
        this.phone = phone;
        this.schedule = schedule;
        this.rating = rating;
        this.title = title;
        this.level = level;
        this.specialties = specialties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getPhone() {
        return phone;
    }

    public String getSchedule() {
        return schedule;
    }

    public Double getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public String getSpecialties() {
        return specialties;
    }
}
