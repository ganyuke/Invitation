package io.github.ganyuke.invitation;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class UninviteCommand implements CommandExecutor, TabCompleter {

    private final Invitation plugin;

    public UninviteCommand(Invitation plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length != 0) {
            player.sendMessage("§cUsage: /" + label);
            return true;
        }

        var result = plugin.getUninviteRegistry().tryRevoke(player.getUniqueId(), plugin.getUndoWindowMs());
        return switch (result.status()) {
            case NOT_FOUND -> {
                player.sendMessage("§cNo recent invite to revoke.");
                yield true;
            }
            case EXPIRED -> {
                player.sendMessage("§cRevocation window for that invite has expired.");
                yield true;
            }
            case CLAIM_HOLDER_MISMATCH -> {
                player.sendMessage("§cThis player has already been re-invited by someone else.");
                yield true;
            }
            case SUCCESS -> {
                OfflinePlayer invited = Bukkit.getOfflinePlayer(result.invitedUuid());
                invited.setWhitelisted(false);

                plugin.getLogger().info(() -> String.format(
                        "Revoked invite for %s (%s) by %s (%s)",
                        result.invitedName(), result.invitedUuid(),
                        player.getName(), player.getUniqueId()
                ));

                player.sendMessage("§aRevoked invite for " + result.invitedName() + ".");
                yield true;
            }
        };
    }
}
