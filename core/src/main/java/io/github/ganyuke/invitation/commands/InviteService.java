package io.github.ganyuke.invitation;

import io.github.ganyuke.invitation.ports.Audience;
import io.github.ganyuke.invitation.ports.LoggerPort;
import io.github.ganyuke.invitation.ports.MessengerPort;
import io.github.ganyuke.invitation.ports.SchedulerPort;
import io.github.ganyuke.invitation.ports.WhitelistPort;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InviteService {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]{3,16}$";

    private final Database database;
    private final UninviteRegistry uninviteRegistry;
    private final InvitationConfig config;
    private final SchedulerPort scheduler;
    private final MessengerPort messenger;
    private final WhitelistPort whitelist;
    private final LoggerPort logger;
    private final Map<UUID, Long> inviteCooldowns = new HashMap<>();

    public InviteService(Database database,
                         UninviteRegistry uninviteRegistry,
                         InvitationConfig config,
                         SchedulerPort scheduler,
                         MessengerPort messenger,
                         WhitelistPort whitelist,
                         LoggerPort logger) {
        this.database = database;
        this.uninviteRegistry = uninviteRegistry;
        this.config = config;
        this.scheduler = scheduler;
        this.messenger = messenger;
        this.whitelist = whitelist;
        this.logger = logger;
    }

    public boolean handleInvite(Audience audience, String label, String username) {
        cleanupCooldowns();

        if (!(audience instanceof Audience.Player inviter)) {
            messenger.sendPlain(audience, "§cOnly players can use this command.");
            return true;
        }

        if (!username.matches(USERNAME_PATTERN)) {
            messenger.sendPlain(audience, "§cThat is not a valid Minecraft username. Use 3-16 letters, numbers, or underscores.");
            return true;
        }

        UUID inviterUuid = inviter.uuid();
        String inviterName = inviter.name();

        if (username.equalsIgnoreCase(inviterName)) {
            messenger.sendPlain(audience, "§cYou can't invite yourself.");
            return true;
        }

        if (isOnCooldown(inviter)) {
            return true;
        }

        // don't let my evil friends cause Mojang to rate-limit me
        startCooldown(inviterUuid);
        messenger.sendPlain(audience, "§7Looking up Minecraft username...");

        scheduler.runAsync(() -> {
            MojangProfile profile;

            try {
                profile = MojangAPI.lookup(username);
            } catch (MojangAPI.LookupException e) {
                int code = e.responseCode();
                logger.warning("Mojang API lookup for '" + username + "' returned HTTP " + code);
                scheduler.runSync(() -> {
                    if (code == 429) {
                        messenger.sendPlainIfOnline(inviterUuid, "§cServer is rate-limited by Mojang. Try again later.");
                    } else if (code == 503) {
                        messenger.sendPlainIfOnline(inviterUuid, "§cMojang's username API is temporarily unavailable. Try again later.");
                    } else {
                        messenger.sendPlainIfOnline(inviterUuid, "§cCould not check that username right now. Try again later.");
                    }
                });
                return;
            } catch (IOException e) {
                logger.warning("Could not check Minecraft username '" + username + "': " + e.getMessage());
                scheduler.runSync(() ->
                        messenger.sendPlainIfOnline(inviterUuid, "§cCould not reach Mojang's API. Try again later.")
                );
                return;
            }

            scheduler.runSync(() -> {
                if (profile == null) {
                    messenger.sendPlainIfOnline(inviterUuid, "§cThat Minecraft username does not exist.");
                    return;
                }

                UUID invitedUuid = profile.uuid();
                String invitedName = profile.name();

                if (whitelist.isWhitelisted(invitedUuid)) {
                    messenger.sendPlainIfOnline(inviterUuid, "§e" + invitedName + " is already whitelisted.");
                    return;
                }

                String invitedUuidString = invitedUuid.toString();

                scheduler.runAsync(() -> {
                    boolean logged = database.logInvite(
                            inviterUuid.toString(),
                            inviterName,
                            invitedUuidString,
                            invitedName
                    );

                    scheduler.runSync(() -> {
                        if (!logged) {
                            messenger.sendPlainIfOnline(inviterUuid, "§cCould not save the invite log. Nobody was whitelisted.");
                            return;
                        }

                        whitelist.add(invitedUuid, invitedName);

                        logger.info(() -> String.format(
                                "Whitelisted %s (%s) invited by %s (%s)",
                                invitedName, invitedUuidString,
                                inviterName, inviterUuid
                        ));

                        uninviteRegistry.record(inviterUuid, invitedUuid, invitedName);
                        messenger.sendInviteSuccess(inviterUuid, invitedName);
                    });
                });
            });
        });

        return true;
    }

    private long cooldownMs() {
        return config.inviteCooldownMs();
    }

    private boolean isOnCooldown(Audience.Player player) {
        long cooldown = cooldownMs();
        if (cooldown <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = inviteCooldowns.getOrDefault(player.uuid(), 0L);
        long remainingMillis = cooldown - (now - last);

        if (remainingMillis > 0) {
            long waitSeconds = Math.max(1, (remainingMillis + 999) / 1000);
            messenger.sendPlain(player, "§cPlease wait " + waitSeconds + "s before inviting again.");
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
}
