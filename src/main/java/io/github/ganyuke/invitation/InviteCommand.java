package io.github.ganyuke.invitation;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteCommand implements CommandExecutor {

    private static final long COOLDOWN_MS = 7_000;
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{3,16}$";

    private final Invitation plugin;
    private final Map<UUID, Long> inviteCooldowns = new HashMap<>();

    public InviteCommand(Invitation plugin) {
        this.plugin = plugin;
    }

    private boolean isOnCooldown(Player player) {
        long now = System.currentTimeMillis();
        long last = inviteCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingMillis = COOLDOWN_MS - (now - last);

        if (remainingMillis > 0) {
            long waitSeconds = Math.max(1, (remainingMillis + 999) / 1000);
            player.sendMessage("§cPlease wait " + waitSeconds + "s before inviting again.");
            return true;
        }

        return false;
    }

    private void startCooldown(UUID playerId) {
        inviteCooldowns.put(playerId, System.currentTimeMillis());
    }

    private void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        inviteCooldowns.entrySet().removeIf(e -> now - e.getValue() > COOLDOWN_MS * 2);
    }

    private void sendIfOnline(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String [] args) {
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
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (e.responseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                        sendIfOnline(inviterUuid, "§cMojang's username API is temporarily unavailable. Try again later.");
                    } else if (e.responseCode() == 429) {
                        sendIfOnline(inviterUuid, "§cServer is rate-limited by Mojang. Try again later.");
                    } else {
                        sendIfOnline(inviterUuid, "§cCould not check that username right now. Mojang API returned HTTP " + e.responseCode() + ".");
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

                if (Bukkit.getWhitelistedPlayers().stream()
                        .anyMatch(p -> p.getUniqueId().equals(invitedUuid))) {

                    sendIfOnline(inviterUuid, "§e" + profile.name() + " is already whitelisted.");
                    return;
                }

                // getOfflinePlayer(UUID) does not perform a name lookup; Mojang already gave us the UUID.
                OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(invitedUuid);
                invitedPlayer.setWhitelisted(true);

                String invitedName = profile.name();
                String invitedUuidString = invitedUuid.toString();

                plugin.getDatabase().logInvite(
                        inviterUuid.toString(),
                        inviterName,
                        invitedUuidString,
                        invitedName
                );

                plugin.getLogger().info(() -> String.format(
                        "Whitelisted %s (%s) invited by %s (%s)",
                        invitedName, invitedUuidString,
                        inviterName, inviterUuid
                ));

                sendIfOnline(inviterUuid, "§aInvited " + invitedName + ". They can now join the server.");
            });
        });

        return true;
    }
}
