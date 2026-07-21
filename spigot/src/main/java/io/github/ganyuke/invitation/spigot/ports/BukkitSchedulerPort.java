package io.github.ganyuke.invitation.spigot.ports;

import io.github.ganyuke.invitation.core.ports.SchedulerPort;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class BukkitSchedulerPort implements SchedulerPort {
    private final Plugin plugin;

    public BukkitSchedulerPort(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
