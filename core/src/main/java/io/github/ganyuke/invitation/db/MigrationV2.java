package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.LoggerPort;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// v2 normalizes names to players table
final class MigrationV2 implements Migration {
    @Override
    public int version() {
        return 2;
    }

    @Override
    public void apply(Connection connection, Path dbPath, LoggerPort logger) throws SQLException {
        if (!tableExists(connection, "invites")) {
            return;
        }

        Migrator.backup(dbPath, logger);
        logger.info("Migrating invite log to players table schema...");
        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version());
            stmt.execute(Schema.CREATE_PLAYERS);
            stmt.execute("ALTER TABLE invites RENAME TO invites_legacy_v1");
            stmt.execute(Schema.CREATE_INVITES);

            int skipped = 0;
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT inviter_uuid, inviter_name, invited_uuid, invited_name, time
                    FROM invites_legacy_v1
                    ORDER BY time ASC
                    """);
                 ResultSet rs = select.executeQuery();
                 PreparedStatement insertInvite = connection.prepareStatement("""
                    INSERT INTO invites (inviter_uuid, invited_uuid, time)
                    VALUES (?, ?, ?)
                    """);
                 PreparedStatement upsertPlayerPs = connection.prepareStatement("""
                    INSERT INTO players (uuid, name, last_seen) VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        name = excluded.name,
                        last_seen = MAX(players.last_seen, excluded.last_seen)
                    """)) {

                while (rs.next()) {
                    String inviterUuid = rs.getString("inviter_uuid");
                    String inviterName = rs.getString("inviter_name");
                    String invitedUuid = rs.getString("invited_uuid");
                    String invitedName = rs.getString("invited_name");
                    long time = rs.getLong("time");

                    if (inviterUuid == null || invitedUuid == null) {
                        skipped++;
                        logger.warning("Skipping legacy invite row with null UUID at time=" + time);
                        continue;
                    }

                    upsertPlayer(upsertPlayerPs, inviterUuid, coalesceName(inviterName, inviterUuid), time);
                    upsertPlayer(upsertPlayerPs, invitedUuid, coalesceName(invitedName, invitedUuid), time);

                    insertInvite.setString(1, inviterUuid);
                    insertInvite.setString(2, invitedUuid);
                    insertInvite.setLong(3, time);
                    insertInvite.executeUpdate();
                }
            }

            if (skipped > 0) {
                logger.warning("Skipped " + skipped + " legacy invite row(s) with null UUIDs; "
                        + "kept invites_legacy_v1 for manual review");
            } else {
                stmt.execute("DROP TABLE invites_legacy_v1");
            }
            connection.commit();
            logger.info("Invite log migration complete.");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void upsertPlayer(PreparedStatement ps, String uuid, String name, long lastSeen)
            throws SQLException {
        ps.setString(1, uuid);
        ps.setString(2, name);
        ps.setLong(3, lastSeen);
        ps.executeUpdate();
    }

    private static boolean tableExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String coalesceName(String name, String uuid) {
        if (name == null || name.isBlank()) {
            return uuid;
        }
        return name;
    }
}
