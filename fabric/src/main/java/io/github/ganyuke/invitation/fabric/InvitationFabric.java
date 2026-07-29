package io.github.ganyuke.invitation.fabric;

import io.github.ganyuke.invitation.common.bootstrap.InvitationBootstrap;
import io.github.ganyuke.invitation.common.commands.InvitationCommands;
import io.github.ganyuke.invitation.common.listeners.PlayerJoinHandler;
import io.github.ganyuke.invitation.fabric.permissions.FabricCommandPermissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class InvitationFabric implements ModInitializer {
    private final InvitationBootstrap bootstrap = new InvitationBootstrap();
    private final Logger logger = LoggerFactory.getLogger("Invitation");

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("invitation");
            if (!bootstrap.start(
                    server,
                    configDir,
                    logger,
                    Executors.newCachedThreadPool(r -> {
                        Thread t = new Thread(r, "Invitation-Async");
                        t.setDaemon(true);
                        return t;
                    }),
                    null
            )) {
                return;
            }
            InvitationCommands.register(
                    server.getCommands().getDispatcher(),
                    bootstrap.core(),
                    new FabricCommandPermissions(logger)
            );
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> bootstrap.stop());

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    if (bootstrap.core() != null) {
                        PlayerJoinHandler.onJoin(bootstrap.core(), handler.player);
                    }
                }
        );
    }
}
