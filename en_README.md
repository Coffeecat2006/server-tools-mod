# server tools mod  ![Mod Icon](src/main/resources/assets/server_tools_mod/icon.png)

**Minecraft:** 1.21.5
**Fabric Loader:** 1.21.5
**Fabric API:** 0.88.0+
**Java:** 17+

A Fabric server-side mod that allows administrators to dynamically create and manage "redeem codes". Players can enter codes to claim custom items and messages, and trigger server commands automatically. It also includes a full-featured mail system so that players can send letters and items to each other.

---

## Table of Contents

* [Features Overview](#features-overview)
* [Installation](#installation)
* [Redeem Code Commands](#redeem-code-commands)

  * [Add / Remove / List](#add--remove--list)
  * [Redeem](#redeem)
  * [Preview](#preview)
  * [Modify Existing Code](#modify-existing-code)

    * [Items](#items)
    * [Fields](#fields)
    * [Events](#events)
  * [View Logs](#view-logs)
  * [Help](#help)
* [Mail Commands](#mail-commands)

  * [Open Mailbox](#open-mailbox)
  * [Send Mail](#send-mail)
  * [Read Mail](#read-mail)
  * [Pickup Packages](#pickup-packages)
  * [Delete Mail](#delete-mail)
  * [Bulk Delete](#bulk-delete)
  * [Blacklist](#blacklist)
  * [Log Query](#log-query)
  * [Help](#mail-help)
* [License](#license)

---

## Features Overview

* **Dynamic Redeem Code Management**: Create, delete, list, and query redeem codes on the fly.
* **Custom Rewards**: Define custom item stacks and text messages.
* **Advanced Editing**: Adjust redeem code properties in real time, including items, messages, claim limits, expiration, and single/multiple-use rules.
* **Hidden Codes**: Mark codes as hidden so players receive "code not found" if they try to use them.
* **Custom Event Hooks**: Execute one or more server commands on code redemption (supports other mod commands). Use `@s` to target the claiming player.
* **Audit Logs**: Full logging of all code creations, edits, and redemptions for auditing and review.
* **Mail System**: Player-to-player mail with items, complete with blacklist, bulk deletion, and logging features.
* **Persistence**: All data auto-saves to the world folder and reloads on server restart.
* **MIT License**: Open source — freely modify and distribute.

---

## Installation

1. Place `server_tools_mod-1.0.3.jar` into your `mods/` folder (Fabric API required).
2. Start the server or client.

---

## Redeem Code Commands

### Add / Remove / List

```shell
# Add a new redeem code
/redeem add <code> "<message>" <limit> <time> <singleUse>
# e.g., single-use, expires after 1440 minutes (24 hours)
/redeem add VIP123 "Welcome, VIP!" 1 1440 true

# Remove a code
/redeem remove <code>

# List all codes (admin only)
/redeem list
```

### Redeem

```shell
# Player redeems a code
/redeem <code>
```

### Preview

```shell
# Preview items for a code (admin only)
/redeem preview item <code>

# Preview text message
/redeem preview text <code>

# Preview events
/redeem preview event <code> all
/redeem preview event <code> <eventName>
```

### Modify Existing Code

Use `/redeem modify <code> <field> [args...]` to adjust an existing code.

#### Items

* `item reset`: Clear the item list for the code.
* `item transform`: Replace the list with whatever the admin is holding in their offhand.
* `item add`: Append the admin’s offhand item to the existing list.

#### Fields

* `code <newCode>`: Rename the code.
* `text "<newMessage>"`: Update the redemption message.
* `limit <number|infinity>`: Set total claim limit (`-1` for unlimited).
* `time <minutes|infinity>`: Set expiration in minutes or `infinity`.
* `rules <true|false>`: Single-use per player (`true`) or allow multiple per player (`false`).
* `receive_status <playerUUID> <true|false>`: Manually mark a player as having claimed (or not).
* `available <true|false>`: Show (`true`) or hide (`false`) this code.

#### Events

* `event add <eventName> <command…>`: Add a server command to run on redemption. Use `@s` for the claiming player.
  e.g. `/redeem modify VIP123 event add greet title @s title "Welcome VIP"`
* `event remove <eventName>`: Remove a named event.
* `event reset`: Clear all events.

### View Logs

```shell
# Show all logs
/redeem log all [recent <n>] [page <p>]

# Show edit history for a code
/redeem log code <code> edits [recent <n>] [page <p>]

# Show redemption history for a code
/redeem log code <code> redeems [recent <n>] [page <p>]

# Show all actions by a player
/redeem log player <player> [recent <n>] [page <p>]
```

### Help

```shell
# Show overview of redeem commands
/redeem help

# Show detailed help for a subcommand
/redeem help <subcommand>
```

---

## Mail Commands

### Open Mailbox

```shell
/mail open [<page>]
```

* Lists the player’s mailbox (5 messages per page). Defaults to page 1.

### Send Mail

```shell
/mail send <player> <title> <content> <item>
```

* `player`: Single player ID or `"@a"` / `"all"` (admin only).
* `title`: Mail subject.
* `content`: Body text (use `\n` for line breaks).
* `item`: `true` to attach the admin’s offhand item, `false` for no attachment.

### Read Mail

```shell
/mail read <id>
```

* View and mark the specified mail as read.

### Pickup Packages

```shell
/mail pickup <id>
```

* Claim attached items from mail.

### Delete Mail

```shell
/mail delete <id>
```

* Prompts `[Confirm] [Cancel]`. Run `/mail delete <id> confirm` to proceed.

### Bulk Delete

```shell
/mail delete all [read|received]
```

* `all`: delete every message.
* `read`: delete all read messages.
* `received`: delete all messages with claimed items.
* Requires confirmation.

### Blacklist

```shell
/mail blacklist
/mail blacklist add <player>
/mail blacklist remove <player>
```

* All changes require confirmation.

### Log Query (Admin)

```shell
/mail log [<page>]
```

* Paginated list of all mail transactions: `[timestamp] sender -> recipient <id> [View]`.

### Help

```shell
/mail help
```

* Show overview of mail commands.

---

## License

This mod is licensed under the MIT License. See the `LICENSE` file for details.