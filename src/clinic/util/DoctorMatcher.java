package clinic.util;

import clinic.model.Doctor;

import java.util.Locale;
import java.util.Objects;

public final class DoctorMatcher {
    private DoctorMatcher() {
    }

    public static boolean matches(String candidate, Doctor doctor) {
        if (doctor == null) {
            return false;
        }
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.trim();
        if (Objects.equals(doctor.getId(), trimmed)) {
            return true;
        }
        if (Objects.equals(doctor.getName(), trimmed)) {
            return true;
        }
        String normalizedDoctor = normalizeName(doctor.getName());
        String normalizedCandidate = normalizeName(trimmed);
        return !normalizedDoctor.isBlank() && normalizedDoctor.equalsIgnoreCase(normalizedCandidate);
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.endsWith("医生")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }
        if (trimmed.toLowerCase(Locale.ROOT).endsWith("doctor")) {
            return trimmed.substring(0, trimmed.length() - 6).trim();
        }
        return trimmed;
    }
}
