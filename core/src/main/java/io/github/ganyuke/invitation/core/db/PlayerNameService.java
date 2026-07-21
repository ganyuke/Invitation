package io.github.ganyuke.invitation.core.db;

import io.github.ganyuke.invitation.core.ports.SchedulerPort;

import java.util.UUID;

public final class PlayerNameService {

    private final Database database;
    private final SchedulerPort scheduler;

    public PlayerNameService(Database database, SchedulerPort scheduler) {
        this.database = database;
        this.scheduler = scheduler;
    }

    public void onPlayerJoin(UUID uuid, String name) {
        scheduler.runAsync(() -> database.refreshPlayerName(uuid, name));
    }
}
