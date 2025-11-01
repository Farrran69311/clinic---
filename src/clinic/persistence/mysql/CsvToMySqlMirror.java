package clinic.persistence.mysql;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CsvToMySqlMirror {
    private static final Logger LOGGER = Logger.getLogger(CsvToMySqlMirror.class.getName());
    private static final CsvToMySqlMirror INSTANCE = new CsvToMySqlMirror();

    private final boolean enabled;

    private CsvToMySqlMirror() {
        boolean shouldEnable = "true".equalsIgnoreCase(System.getenv().getOrDefault("CLINIC_DB_SYNC_ENABLED", "true"))
            || Boolean.getBoolean("clinic.db.sync.enabled");
        if (!MySqlConnectionManager.isDriverAvailable()) {
            shouldEnable = false;
            LOGGER.warning("MySQL JDBC driver not found; CSV -> MySQL 同步已禁用");
        }
        this.enabled = shouldEnable;
    }

    public static CsvToMySqlMirror getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void mirror(Path csvFile, String header, List<String[]> records) throws IOException {
        if (!enabled) {
            return;
        }
        String tableName = deriveTableName(csvFile);
        String[] columns = header.split("\\|", -1);
        List<String[]> normalizedRecords = normalizeRecords(records, columns.length);
        try (Connection connection = MySqlConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            truncateTable(connection, tableName);
            if (!normalizedRecords.isEmpty()) {
                bulkInsert(connection, tableName, columns, normalizedRecords);
            }
            connection.commit();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "同步 CSV 至 MySQL 失败: " + tableName, ex);
            throw new IOException("无法同步数据到 MySQL 表: " + tableName, ex);
        }
    }

    private String deriveTableName(Path csvFile) {
        String filename = csvFile.getFileName().toString();
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        return filename;
    }

    private List<String[]> normalizeRecords(List<String[]> records, int columnCount) {
        List<String[]> normalized = new ArrayList<>(records.size());
        for (String[] record : records) {
            String[] copy = Arrays.copyOf(record, columnCount);
            for (int i = 0; i < columnCount; i++) {
                if (copy[i] == null) {
                    copy[i] = "";
                }
            }
            normalized.add(copy);
        }
        return normalized;
    }

    private void truncateTable(Connection connection, String tableName) throws SQLException {
        String sql = String.format(Locale.ROOT, "DELETE FROM `%s`", tableName);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private void bulkInsert(Connection connection,
                            String tableName,
                            String[] columns,
                            List<String[]> records) throws SQLException {
        StringBuilder columnPart = new StringBuilder();
        StringBuilder placeholderPart = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                columnPart.append(',');
                placeholderPart.append(',');
            }
            columnPart.append('`').append(columns[i]).append('`');
            placeholderPart.append('?');
        }
        String sql = String.format(Locale.ROOT,
            "INSERT INTO `%s` (%s) VALUES (%s)",
            tableName,
            columnPart,
            placeholderPart
        );
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] record : records) {
                for (int i = 0; i < columns.length; i++) {
                    String value = record[i] == null ? "" : record[i];
                    if (value.isEmpty()) {
                        statement.setObject(i + 1, null);
                    } else {
                        statement.setString(i + 1, value);
                    }
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
