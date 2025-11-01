package clinic.persistence.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public final class MySqlConnectionManager {
    private static final String HOST;
    private static final String PORT;
    private static final String DATABASE;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final boolean DRIVER_AVAILABLE;

    static {
        HOST = getConfig("CLINIC_DB_HOST", "clinic.db.host", "localhost");
        PORT = getConfig("CLINIC_DB_PORT", "clinic.db.port", "3306");
        DATABASE = getConfig("CLINIC_DB_NAME", "clinic.db.name", "clinic");
        USERNAME = getConfig("CLINIC_DB_USER", "clinic.db.user", "root");
        PASSWORD = getConfig("CLINIC_DB_PASSWORD", "clinic.db.password", "123456");
        boolean driverLoaded;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException ex) {
            driverLoaded = false;
        }
        DRIVER_AVAILABLE = driverLoaded;
    }

    private MySqlConnectionManager() {
    }

    public static boolean isDriverAvailable() {
        return DRIVER_AVAILABLE;
    }

    public static Connection getConnection() throws SQLException {
        if (!DRIVER_AVAILABLE) {
            throw new SQLException("MySQL JDBC driver not found on classpath");
        }
        String url = String.format(Locale.ROOT,
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            HOST,
            PORT,
            DATABASE
        );
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }

    private static String getConfig(String envKey, String sysKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        String sysValue = System.getProperty(sysKey);
        if (sysValue != null && !sysValue.isEmpty()) {
            return sysValue;
        }
        return Objects.toString(defaultValue, "");
    }
}
