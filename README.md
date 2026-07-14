# Invitation

Let your friends invite their friends without your knowledge! In less cynical terms, no longer will your friends have to beg you to whitelist their friends for the annual summer Minecraft survival multiplayer server. Tell them to do it themselves!

A Spigot (or Paper, or Purpur, or whatever Spigot derivative you want) plugin for Minecraft 1.17 to 26.1.2. Tested to work on Spigot 1.17 and Paper 26.1.2.

This is a pretty wrapper around vanilla functionality. If there's something more than invites that you expect this plugin to do, it almost certainly already exists in vanilla: blacklists (`/ban`), kick on uninvite (`enforce-whitelist=true`), and invite revocation (`/whitelist remove`).

## Features

- Let anyone whitelist a player by username. No more pings to add friends to the summer SMP!
- Revoke mistaken invites within a configurable window. Typos have been forgiven!
- (For operators) Look up who invited whom in the invite log to figure out who to hold accountable!

## Installation

1. Download the `.jar` file from GitHub releases or [Modrinth](https://modrinth.com/plugin/invitation).
2. Place it in your `/plugins/` directory in your server root.
3. Start your server.

## User guide

<details>
  <summary>Click to view Frequently Asked Questions</summary>

> **How do I invite someone?**
> Use `/invite <username>`

> **I mistakenly invited the wrong person. How do I uninvite someone?**
> Use `/uninvite` to revoke your most recent invite. Beyond that, you need an admin to `/whitelist remove <username>` them.

> **I accidentally invited someone and now they are on the server. What do I do?**
> First, revoke their invite if you can. Otherwise, ask the admin to remove them from the whitelist then `/kick <username>` them. You should set `enforce-whitelist=true` in your `server.properties` if you want to kick uninvited players immediately.

> **How do I figure out who invited a griefer to the server?**
> Use `/invitelog received <username>` using the username of the griefer. The latest entry is your culprit.

> **My friend keeps inviting someone I don't like. How do I blacklist that player from being invited?**
> This exists in vanilla already: Just ban that problematic player (not your friend, but you can do that too) with `/ban <username>`. They can still be invited, but they can't join.

> **My friend got upset and invited a crap ton of randoms. How do I rollback the whitelist?**
> There is no rollback. You should've kept a copy of `whitelist.json` as a backup. More importantly, you should have a sit down with your friend.

> **Can I use this on an offline server?**
> Sure, but it only whitelists real Minecraft accounts. Behind Velocity, this works fine. A real offline server? No.

</details>

## Commands
<details>
  <summary>Click to view command list</summary>

| Command      | Usage                                        | Permission   | Who can use it by default | What it does                                                                                        |
| ------------ | -------------------------------------------- | ------------ | ------------------------- | --------------------------------------------------------------------------------------------------- |
| `/invite`    | `/invite <player>`                           | `invite.use` | Everyone                  | Looks up a Minecraft username, adds that player to the server whitelist, and logs who invited them. |
| `/uninvite`  | `/uninvite`                                  | `invite.use` | Everyone                  | Revokes your recent invite if still inside the undo window.                   |
| `/invitelog` | `/invitelog`                                 | `invite.log` | Operators                 | Alias for `/invitelog recent 1`.                                                                    |
| `/invitelog` | `/invitelog recent [page]`                   | `invite.log` | Operators                 | Shows recent invite log entries, 5 per page.                                                       |
| `/invitelog` | `/invitelog sent <player> [page]`            | `invite.log` | Operators                 | Shows invites sent by that player, 5 per page.                                                     |
| `/invitelog` | `/invitelog received <player> [page]`        | `invite.log` | Operators                 | Shows who invited that player, 5 per page.                                                         |

</details>

## Configuration

<details>
  <summary>Click to view configuration options</summary>

You can edit the plugin's configuration in `plugins/Invitation/config.yml`:

| Key                 | Default | What it does                                                         |
| ------------------- | ------- | -------------------------------------------------------------------- |
| `cooldown-seconds`  | `7`     | Wait between `/invite` attempts per player.                          |
| `undo-seconds`      | `60`    | How long after inviting `/uninvite` (or Undo) can revoke that invite. |

</details>

## Contributing

If you wrote the code for it, I'll happily accept a pull request if I feel like it doesn't bloat the scope of this plugin. 

By submitting a contribution to this repository, you agree that your contribution is licensed under the same license as this repository, as published in the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/Invitation/refs/heads/mailbox/LICENSE) file.

## License

Unless otherwise noted, all source code in this repository is licensed under the **Mozilla Public License 2.0** (SPDX: **MPL-2.0**). Please view the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/Invitation/refs/heads/mailbox/LICENSE) file for the terms you are afforded under the MPL-2.0.
