package clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private final String id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final LocalDateTime createdAt;

    public User(String id, String username, String passwordHash, Role role, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
