package io.github.ganyuke.invitation;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Database {

    private final String url;
    private final ExecutorService executor;
    private Connection connection;

    public Database(File dataFolder) {
        var dataDir = dataFolder.toPath();
        Path dbPath = dataDir.resolve("invites.db");
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void init() {
        try {
            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS invites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        inviter_uuid TEXT,
                        inviter_name TEXT,
                        invited_uuid TEXT,
                        invited_name TEXT,
                        time INTEGER
                    )
                """);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void logInvite(String inviterUUID, String inviterName,
                                String invitedUUID, String invitedName) {
        executor.execute(() -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO invites (inviter_uuid, inviter_name, invited_uuid, invited_name, time)
                        VALUES (?, ?, ?, ?, ?)
                     """)) {

                    ps.setString(1, inviterUUID);
                    ps.setString(2, inviterName);
                    ps.setString(3, invitedUUID);
                    ps.setString(4, invitedName);
                    ps.setLong(5, System.currentTimeMillis());
                    ps.execute();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public synchronized List<InviteLogEntry> getLogs(String playerName) throws SQLException {
        List<InviteLogEntry> entries = new ArrayList<>();

        String sql = playerName == null
                ? """
              SELECT inviter_uuid, inviter_name, invited_uuid, invited_name, time
              FROM invites
              ORDER BY time DESC
              LIMIT 10
              """
                : """
              SELECT inviter_uuid, inviter_name, invited_uuid, invited_name, time
              FROM invites
              WHERE LOWER(invited_name) = LOWER(?)
              ORDER BY time DESC
              LIMIT 10
              """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            if (playerName != null) {
                ps.setString(1, playerName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new InviteLogEntry(
                            rs.getString("inviter_uuid"),
                            rs.getString("inviter_name"),
                            rs.getString("invited_uuid"),
                            rs.getString("invited_name"),
                            rs.getLong("time")
                    ));
                }
            }
        }

        return entries;
    }

    public synchronized List<String> getInvitedPlayerNames(String query) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT DISTINCT invited_name FROM invites
                WHERE LOWER(invited_name) LIKE LOWER(?)
                LIMIT 20
            """)) {
            ps.setString(1, query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("invited_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }


}
