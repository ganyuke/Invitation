package io.github.ganyuke.invitation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

// migrates DB to latest schema based on pragma user_version
final class Migrator {

    private static final List<Migration> MIGRATIONS = List.of(
            new MigrationV2()
    );

    private final Path dbPath;
    private final Logger logger;

    Migrator(Path dbPath, Logger logger) {
        this.dbPath = dbPath;
        this.logger = logger;
    }

    void migrateToCurrent(Connection connection) throws SQLException {
        int current = getUserVersion(connection);
        if (current >= Schema.VERSION) {
            return;
        }

        for (Migration migration : MIGRATIONS) {
            if (current < migration.version()) {
                migration.apply(connection, dbPath, logger);
                current = getUserVersion(connection);
            }
        }

        if (getUserVersion(connection) < Schema.VERSION) {
            connection.setAutoCommit(false);
            try {
                setUserVersion(connection, Schema.VERSION);
                Schema.ensureCurrentTables(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    static void backup(Path dbPath, Logger logger) throws SQLException {
        Path bak = dbPath.resolveSibling("invites.db.bak");
        try {
            Files.copy(dbPath, bak, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Wrote pre-migration backup to " + bak.getFileName());
        } catch (IOException e) {
            throw new SQLException("Failed to write " + bak.getFileName() + " before migration", e);
        }
    }

    private static int getUserVersion(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void setUserVersion(Connection connection, int version) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version);
        }
    }
}
