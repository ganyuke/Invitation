package io.github.ganyuke.invitation.core.db;

import io.github.ganyuke.invitation.core.ports.LoggerPort;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

interface Migration {
    // version of the schema after this migration succeeds.
    int version();

    void apply(Connection connection, Path dbPath, LoggerPort logger) throws SQLException;
}
