package io.github.ganyuke.invitation;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class Schema {
    static final int VERSION = 2;

    static final String CREATE_PLAYERS = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                last_seen INTEGER NOT NULL
            )
            """;

    static final String CREATE_INVITES = """
            CREATE TABLE IF NOT EXISTS invites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                inviter_uuid TEXT NOT NULL,
                invited_uuid TEXT NOT NULL,
                time INTEGER NOT NULL,
                FOREIGN KEY (inviter_uuid) REFERENCES players(uuid),
                FOREIGN KEY (invited_uuid) REFERENCES players(uuid)
            )
            """;

    private Schema() {
    }

    static void ensureCurrentTables(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_PLAYERS);
            stmt.execute(CREATE_INVITES);
        }
    }
}
