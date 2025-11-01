package clinic.model;

import java.time.LocalDate;
import java.util.Objects;

public class Patient {
    private final String id;
    private final String name;
    private final String gender;
    private final LocalDate birthday;
    private final String phone;
    private final String address;
    private final String emergencyContact;
    private final String notes;

    public Patient(String id, String name, String gender, LocalDate birthday, String phone, String address, String emergencyContact, String notes) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.gender = gender;
        this.birthday = birthday;
        this.phone = phone;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.notes = notes == null ? "" : notes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public String getNotes() {
        return notes;
    }
}
