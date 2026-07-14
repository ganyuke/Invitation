package io.github.ganyuke.invitation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InviteCommand implements CommandExecutor, TabCompleter {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{3,16}$";

    private final Invitation plugin;
    private final Map<UUID, Long> inviteCooldowns = new HashMap<>();

    public InviteCommand(Invitation plugin) {
        this.plugin = plugin;
    }

    private long cooldownMs() {
        return plugin.getInviteCooldownMs();
    }

    private boolean isOnCooldown(Player player) {
        long cooldown = cooldownMs();
        if (cooldown <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = inviteCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingMillis = cooldown - (now - last);

        if (remainingMillis > 0) {
            long waitSeconds = Math.max(1, (remainingMillis + 999) / 1000);
            player.sendMessage("§cPlease wait " + waitSeconds + "s before inviting again.");
            return true;
        }

        return false;
    }

    private void startCooldown(UUID playerId) {
        if (cooldownMs() <= 0) {
            return;
        }
        inviteCooldowns.put(playerId, System.currentTimeMillis());
    }

    private void cleanupCooldowns() {
        long cooldown = cooldownMs();
        if (cooldown <= 0) {
            inviteCooldowns.clear();
            return;
        }
        long now = System.currentTimeMillis();
        inviteCooldowns.entrySet().removeIf(e -> now - e.getValue() > cooldown * 2);
    }

    private void sendIfOnline(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    private void sendInviteSuccess(UUID inviterUuid, String invitedName, UUID invitedUuid) {
        plugin.getUninviteRegistry().record(inviterUuid, invitedUuid, invitedName);

        Player player = Bukkit.getPlayer(inviterUuid);
        if (player == null) {
            return;
        }

        TextComponent message = new TextComponent("Invited " + invitedName + ". They are now whitelisted on the server. ");
        message.setColor(ChatColor.GREEN);

        TextComponent undo = new TextComponent("Undo");
        undo.setColor(ChatColor.YELLOW);
        undo.setUnderlined(true);
        undo.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/uninvite"));
        undo.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Revoke this invite").create()
        ));

        player.spigot().sendMessage(message, undo);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        cleanupCooldowns();

        if (!(sender instanceof Player inviter)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length != 1) {
            inviter.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }

        String username = args[0];
        if (!username.matches(USERNAME_PATTERN)) {
            inviter.sendMessage("§cThat is not a valid Minecraft username. Use 3-16 letters, numbers, or underscores.");
            return true;
        }

        UUID inviterUuid = inviter.getUniqueId();
        String inviterName = inviter.getName();

        if (username.equalsIgnoreCase(inviterName)) {
            inviter.sendMessage("§cYou can't invite yourself.");
            return true;
        }

        if (isOnCooldown(inviter)) {
            return true;
        }

        // don't let my evil friends cause Mojang to rate-limit me
        startCooldown(inviterUuid);
        inviter.sendMessage("§7Looking up Minecraft username...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MojangProfile profile;

            try {
                profile = MojangAPI.lookup(username);
            } catch (MojangAPI.LookupException e) {
                int code = e.responseCode();
                plugin.getLogger().warning("Mojang API lookup for '" + username + "' returned HTTP " + code);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (code == 429) {
                        sendIfOnline(inviterUuid, "§cServer is rate-limited by Mojang. Try again later.");
                    } else if (code == 503) {
                        sendIfOnline(inviterUuid, "§cMojang's username API is temporarily unavailable. Try again later.");
                    } else {
                        sendIfOnline(inviterUuid, "§cCould not check that username right now. Try again later.");
                    }
                });
                return;
            } catch (IOException e) {
                plugin.getLogger().warning("Could not check Minecraft username '" + username + "': " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                        sendIfOnline(inviterUuid, "§cCould not reach Mojang's API. Try again later.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile == null) {
                    sendIfOnline(inviterUuid, "§cThat Minecraft username does not exist.");
                    return;
                }

                UUID invitedUuid = profile.uuid();
                String invitedName = profile.name();

                if (Bukkit.getWhitelistedPlayers().stream()
                        .anyMatch(p -> p.getUniqueId().equals(invitedUuid))) {
                    sendIfOnline(inviterUuid, "§e" + invitedName + " is already whitelisted.");
                    return;
                }

                String invitedUuidString = invitedUuid.toString();
                // getOfflinePlayer(UUID) does not perform a name lookup; Mojang already gave us the UUID.
                OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(invitedUuid);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean logged = plugin.getDatabase().logInvite(
                            inviterUuid.toString(),
                            inviterName,
                            invitedUuidString,
                            invitedName
                    );

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!logged) {
                            sendIfOnline(inviterUuid, "§cCould not save the invite log. Nobody was whitelisted.");
                            return;
                        }

                        invitedPlayer.setWhitelisted(true);
                        WhitelistFile.setEntryName(invitedUuid, invitedName);

                        plugin.getLogger().info(() -> String.format(
                                "Whitelisted %s (%s) invited by %s (%s)",
                                invitedName, invitedUuidString,
                                inviterName, inviterUuid
                        ));

                        sendInviteSuccess(inviterUuid, invitedName, invitedUuid);
                    });
                });
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return List.of();
    }
}
