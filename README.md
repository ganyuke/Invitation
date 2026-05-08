# Invitation

Let your friends invite their friends without your knowledge! In less cynical terms, no longer will your friends have to beg you to whitelist their friends for the annual summer Minecraft survival multiplayer server. Tell them to do it themselves!

A Spigot (or Paper, or Purpur, or whatever Spigot derivative you want) plugin for Minecraft 1.17 to 26.1.2. Tested to work on Spigot 1.17 and Paper 26.1.2.

## Configuration

There is no configuration. Probably should be able to configure the cooldown... but maybe not right now. It's a dead simple plugin.

## Commands

| Command      | Usage                 | Permission   | Who can use it by default | What it does                                                                                        |
| ------------ | --------------------- | ------------ | ------------------------- | --------------------------------------------------------------------------------------------------- |
| `/invite`    | `/invite <player>`    | `invite.use` | Everyone                  | Looks up a Minecraft username, adds that player to the server whitelist, and logs who invited them. |
| `/invitelog` | `/invitelog`          | `invite.log` | Operators                 | Shows the 10 most recent invite log entries.                                                        |
| `/invitelog` | `/invitelog <player>` | `invite.log` | Operators                 | Shows recent invite log entries where the specified player was invited.                             |

## Installation

1. Download the `.jar` file from GitHub releases or Modrinth (soon).
2. Place it in your `/plugins/` directory in your server root.
3. Start your server.

## Contributing

If you wrote the code for it, I'll happily accept a pull request if I feel like it doesn't bloat the scope of this plugin. 

By submitting a contribution to this repository, you agree that your contribution is licensed under the same license as this repository, as published in the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/Invitation/refs/heads/mailbox/LICENSE) file.

## License

Unless otherwise noted, all source code in this repository is licensed under the **Mozilla Public License 2.0** (SPDX: **MPL-2.0**). Please view the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/Invitation/refs/heads/mailbox/LICENSE) file for the terms you are afforded under the MPL-2.0.

