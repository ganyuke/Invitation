package io.github.ganyuke.invitation;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerNameListener implements Listener {

    private final Invitation plugin;

    public PlayerNameListener(Invitation plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabase().refreshPlayerName(player.getUniqueId(), player.getName())
        );
    }
}
