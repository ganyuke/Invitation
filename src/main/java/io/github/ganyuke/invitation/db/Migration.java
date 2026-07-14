package io.github.ganyuke.invitation;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

interface Migration {
    // version of the schema after this migration succeeds.
    int version();

    void apply(Connection connection, Path dbPath, Logger logger) throws SQLException;
}
