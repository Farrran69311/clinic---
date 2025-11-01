package clinic.persistence;

import clinic.model.Role;
import clinic.model.User;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final String HEADER = "id|username|passwordHash|role|createdAt";

    private final Path file;

    public UserRepository(Path file) {
        this.file = file;
    }

    public List<User> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<User> users = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 5) {
                continue;
            }
            users.add(new User(
                row[0],
                row[1],
                row[2],
                Role.valueOf(row[3]),
                LocalDateTime.parse(row[4])
            ));
        }
        return users;
    }

    public Optional<User> findByUsername(String username) throws IOException {
        return findAll().stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username))
            .findFirst();
    }

    public void save(User user) throws IOException {
        List<User> users = findAll();
        boolean updated = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                updated = true;
                break;
            }
        }
        if (!updated) {
            users.add(user);
        }
        write(users);
    }

    public void deleteById(String id) throws IOException {
        List<User> users = findAll();
        users.removeIf(u -> u.getId().equals(id));
        write(users);
    }

    private void write(List<User> users) throws IOException {
        List<String[]> rows = new ArrayList<>(users.size());
        for (User user : users) {
            rows.add(new String[]{
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getCreatedAt().toString()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
