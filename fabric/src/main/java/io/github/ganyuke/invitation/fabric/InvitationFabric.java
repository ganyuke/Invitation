package io.github.ganyuke.invitation.fabric;

import io.github.ganyuke.invitation.common.InvitationBootstrap;
import io.github.ganyuke.invitation.common.InvitationCommands;
import io.github.ganyuke.invitation.common.PlayerJoinHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class InvitationFabric implements ModInitializer {
    private final InvitationBootstrap bootstrap = new InvitationBootstrap();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("invitation");
            if (!bootstrap.start(
                    server,
                    configDir,
                    LoggerFactory.getLogger("Invitation"),
                    Executors.newCachedThreadPool(r -> {
                        Thread t = new Thread(r, "Invitation-Async");
                        t.setDaemon(true);
                        return t;
                    })
            )) {
                return;
            }
            InvitationCommands.register(
                    server.getCommands().getDispatcher(),
                    bootstrap.core(),
                    new FabricCommandPermissions()
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
