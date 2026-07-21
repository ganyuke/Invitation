package io.github.ganyuke.invitation.spigot.commands;

import io.github.ganyuke.invitation.core.InvitationCore;
import io.github.ganyuke.invitation.spigot.ports.BukkitLoggerPort;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class UninviteCommand implements CommandExecutor, TabCompleter {
    private final InvitationCore core;

    public UninviteCommand(InvitationCore core) {
        this.core = core;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 0) {
            sender.sendMessage("§cUsage: /" + label);
            return true;
        }
        return core.uninviteService().handleUninvite(BukkitLoggerPort.toAudience(sender));
    }
}
