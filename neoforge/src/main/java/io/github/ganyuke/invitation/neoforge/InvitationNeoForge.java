package io.github.ganyuke.invitation.neoforge;

import dev.axionize.sqlite_jdbc.MinecraftSqliteJdbc;
import io.github.ganyuke.invitation.common.bootstrap.InvitationBootstrap;
import io.github.ganyuke.invitation.common.commands.InvitationCommands;
import io.github.ganyuke.invitation.common.listeners.PlayerJoinHandler;
import io.github.ganyuke.invitation.neoforge.permissions.NeoForgeCommandPermissions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;

@Mod(InvitationNeoForge.MOD_ID)
public final class InvitationNeoForge {
    public static final String MOD_ID = "invitation";
    private static final Logger LOGGER = LoggerFactory.getLogger("Invitation");
    private static final InvitationBootstrap BOOTSTRAP = new InvitationBootstrap();

    public InvitationNeoForge() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("invitation");

        Connection conn;
        try {
            conn = MinecraftSqliteJdbc.connect("jdbc:sqlite:" + configDir.resolve("invites.db").toAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (!BOOTSTRAP.start(
                event.getServer(),
                configDir,
                LOGGER,
                Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "Invitation-Async");
                    t.setDaemon(true);
                    return t;
                }),
                conn
        )) {
            return;
        }
        InvitationCommands.register(
                event.getServer().getCommands().getDispatcher(),
                BOOTSTRAP.core(),
                new NeoForgeCommandPermissions()
        );
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BOOTSTRAP.stop();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (BOOTSTRAP.core() != null && event.getEntity() instanceof ServerPlayer player) {
            PlayerJoinHandler.onJoin(BOOTSTRAP.core(), player);
        }
    }
}