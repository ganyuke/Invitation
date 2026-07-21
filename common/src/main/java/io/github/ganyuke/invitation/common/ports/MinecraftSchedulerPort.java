package io.github.ganyuke.invitation.common.ports;

import io.github.ganyuke.invitation.core.ports.SchedulerPort;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executor;

public final class MinecraftSchedulerPort implements SchedulerPort {
    private final MinecraftServer server;
    private final Executor asyncExecutor;

    public MinecraftSchedulerPort(MinecraftServer server, Executor asyncExecutor) {
        this.server = server;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void runAsync(Runnable task) {
        asyncExecutor.execute(task);
    }

    @Override
    public void runSync(Runnable task) {
        server.execute(task);
    }
}
