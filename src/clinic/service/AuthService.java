package clinic.service;

import clinic.model.Patient;
import clinic.model.Role;
import clinic.model.User;
import clinic.persistence.CsvDataStore;
import clinic.persistence.PatientRepository;
import clinic.persistence.UserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public AuthService(UserRepository userRepository, PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    public Optional<User> login(String username, String password) throws IOException {
        String hash = hash(password);
        return userRepository.findByUsername(username)
            .filter(user -> user.getPasswordHash().equals(hash));
    }

    public boolean userExists(String username) throws IOException {
        return userRepository.findByUsername(username).isPresent();
    }

    public User registerPatient(String username, String password) throws IOException {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(
            CsvDataStore.randomId(),
            username,
            hash(password),
            Role.PATIENT,
            LocalDateTime.now()
        );
        userRepository.save(user);
        if (patientRepository != null) {
            Patient patient = new Patient(
                user.getId(),
                username,
                "",
                null,
                "",
                "",
                "",
                ""
            );
            patientRepository.save(patient);
        }
        return user;
    }

    public User createDoctorAccount(String username, String password) throws IOException {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User(
            CsvDataStore.randomId(),
            username,
            hash(password),
            Role.DOCTOR,
            LocalDateTime.now()
        );
        userRepository.save(user);
        return user;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("不支持的哈希算法", e);
        }
    }
}
