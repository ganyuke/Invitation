package io.github.ganyuke.invitation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerNameListener implements Listener {
    private final InvitationCore core;

    public PlayerNameListener(InvitationCore core) {
        this.core = core;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        core.playerNameService().onPlayerJoin(player.getUniqueId(), player.getName());
    }
}
