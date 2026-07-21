package io.github.ganyuke.invitation.spigot.commands;

import io.github.ganyuke.invitation.core.InvitationCore;
import io.github.ganyuke.invitation.spigot.ports.BukkitLoggerPort;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class InviteCommand implements CommandExecutor, TabCompleter {
    private final InvitationCore core;

    public InviteCommand(InvitationCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }
        return core.inviteService().handleInvite(BukkitLoggerPort.toAudience(sender), label, args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return List.of();
    }
}
