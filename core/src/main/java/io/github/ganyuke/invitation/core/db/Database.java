package io.github.ganyuke.invitation.core.db;

import io.github.ganyuke.invitation.core.SqliteUnavailableException;
import io.github.ganyuke.invitation.core.commands.InviteLogEntry;
import io.github.ganyuke.invitation.core.commands.InviteLogPage;
import io.github.ganyuke.invitation.core.ports.LoggerPort;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Database {

    public static final int PAGE_SIZE = 5;

    private final Path dbPath;
    private final String url;
    private final LoggerPort logger;
    private final Set<String> knownPlayerNames = ConcurrentHashMap.newKeySet();
    private Connection connection;

    public Database(Path dataFolder, LoggerPort logger, Connection connection) {
        this.dbPath = dataFolder.resolve("invites.db");
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        this.logger = logger;
        this.connection = connection;
    }

    public void init() throws SqliteUnavailableException {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                logger.warning("Missing SQLite JDBC driver. Recommend installing minecraft-sqlite-jdbc");
                throw new SqliteUnavailableException(e);
            }
        }

        try {
            if (connection == null)
                connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            new Migrator(dbPath, logger).migrateToCurrent(connection);
            loadNameCache();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    private void loadNameCache() throws SQLException {
        knownPlayerNames.clear();
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM players");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    knownPlayerNames.add(name);
                }
            }
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.severe("Failed to close invite database: " + e.getMessage());
            }
        }
    }

    /** Refresh name for an existing player only (e.g. on join). */
    public synchronized void refreshPlayerName(UUID uuid, String name) {
        try {
            String uuidString = uuid.toString();
            String previous = null;
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT name FROM players WHERE uuid = ?")) {
                select.setString(1, uuidString);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }
                    previous = rs.getString("name");
                }
            }

            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE players SET name = ?, last_seen = ? WHERE uuid = ?")) {
                update.setString(1, name);
                update.setLong(2, System.currentTimeMillis());
                update.setString(3, uuidString);
                update.executeUpdate();
            }

            if (previous != null && !previous.equals(name)) {
                knownPlayerNames.remove(previous);
            }
            knownPlayerNames.add(name);
        } catch (SQLException e) {
            logger.severe("Failed to refresh player name " + name + " (" + uuid + "): " + e.getMessage());
        }
    }

    private void upsertPlayer(String uuid, String name) throws SQLException {
        String previous = null;
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT name FROM players WHERE uuid = ?")) {
            select.setString(1, uuid);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    previous = rs.getString("name");
                }
            }
        }

        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO players (uuid, name, last_seen) VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    last_seen = MAX(players.last_seen, excluded.last_seen)
                """)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setLong(3, now);
            ps.executeUpdate();
        }

        if (previous != null && !previous.equals(name)) {
            knownPlayerNames.remove(previous);
        }
        knownPlayerNames.add(name);
    }

    /** Persist an invite. Returns {@code true} only if the row was written. */
    public synchronized boolean logInvite(String inviterUUID, String inviterName,
                                          String invitedUUID, String invitedName) {
        try {
            connection.setAutoCommit(false);
            try {
                upsertPlayer(inviterUUID, inviterName);
                upsertPlayer(invitedUUID, invitedName);

                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO invites (inviter_uuid, invited_uuid, time)
                        VALUES (?, ?, ?)
                     """)) {
                    ps.setString(1, inviterUUID);
                    ps.setString(2, invitedUUID);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                loadNameCache();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("Failed to log invite of " + invitedName + " by " + inviterName + ": " + e.getMessage());
            return false;
        }
    }

    public synchronized InviteLogPage getRecentLogs(int page) throws SQLException {
        return paginate(queryLogs("""
                SELECT i.inviter_uuid, inviter.name AS inviter_name,
                       i.invited_uuid, invited.name AS invited_name, i.time
                FROM invites i
                JOIN players inviter ON inviter.uuid = i.inviter_uuid
                JOIN players invited ON invited.uuid = i.invited_uuid
                ORDER BY i.time DESC
                LIMIT ? OFFSET ?
                """, PAGE_SIZE + 1, offsetForPage(page)));
    }

    public synchronized InviteLogPage getSentLogs(String inviterName, int page) throws SQLException {
        String uuid = findUuidByName(inviterName);
        if (uuid == null) {
            return InviteLogPage.empty();
        }
        return paginate(queryLogs("""
                SELECT i.inviter_uuid, inviter.name AS inviter_name,
                       i.invited_uuid, invited.name AS invited_name, i.time
                FROM invites i
                JOIN players inviter ON inviter.uuid = i.inviter_uuid
                JOIN players invited ON invited.uuid = i.invited_uuid
                WHERE i.inviter_uuid = ?
                ORDER BY i.time DESC
                LIMIT ? OFFSET ?
                """, uuid, PAGE_SIZE + 1, offsetForPage(page)));
    }

    public synchronized InviteLogPage getReceivedLogs(String invitedName, int page) throws SQLException {
        String uuid = findUuidByName(invitedName);
        if (uuid == null) {
            return InviteLogPage.empty();
        }
        return paginate(queryLogs("""
                SELECT i.inviter_uuid, inviter.name AS inviter_name,
                       i.invited_uuid, invited.name AS invited_name, i.time
                FROM invites i
                JOIN players inviter ON inviter.uuid = i.inviter_uuid
                JOIN players invited ON invited.uuid = i.invited_uuid
                WHERE i.invited_uuid = ?
                ORDER BY i.time DESC
                LIMIT ? OFFSET ?
                """, uuid, PAGE_SIZE + 1, offsetForPage(page)));
    }

    private static InviteLogPage paginate(List<InviteLogEntry> fetched) {
        if (fetched.size() > PAGE_SIZE) {
            return new InviteLogPage(List.copyOf(fetched.subList(0, PAGE_SIZE)), true);
        }
        return new InviteLogPage(List.copyOf(fetched), false);
    }

    private String findUuidByName(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM players WHERE LOWER(name) = LOWER(?) ORDER BY last_seen DESC LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        }
        return null;
    }

    private static int offsetForPage(int page) {
        return (page - 1) * PAGE_SIZE;
    }

    private List<InviteLogEntry> queryLogs(String sql, Object... params) throws SQLException {
        List<InviteLogEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof String s) {
                    ps.setString(i + 1, s);
                } else if (param instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    throw new IllegalArgumentException("Unsupported SQL param: " + param);
                }
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

    /** Cache for tab completion. */
    public List<String> getKnownPlayerNames(String query) {
        String prefix = query.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (String name : knownPlayerNames) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(name);
                if (names.size() >= 20) {
                    break;
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
