# Locksmith

Locksmith is a Fabric mod for Minecraft 26.3 that adds simple vanilla style player shops.

Player can create/manage thier own shops and admins can also acces them via commands if that is nececary

You can create a shop using the `/locksmith sell 10 minecraft:diamond` or by renaming a chest in an anvil `10 diamond`

Admins can also set regions where shops can be placed and there is also a default region around the world spawn of 100 blocks

## Versions

- Minecraft: 26.3
- Fabric Loader: 0.20.1+
- Java: 25+

## Build

```bash
./gradlew build
```

## Commands

Every command also works with the short alias `/ls`, for example `/ls sell 10 minecraft:diamond`.

### Player Commands

| Command | Description |
|---------|-------------|
| `/locksmith sell <amount> <item>` | Prepares the chest in your hand as a shop sell chest |
| Rename a chest in anvil to `<amount> <item>` | Alternative way to create a shop chest (e.g. `10 diamond`) |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/locksmith admin` | Toggles admin shop access mode allows opening any shop chest |
| `/locksmith region set pos1` | Sets the first corner of a new shop region at your current position |
| `/locksmith region set pos2` | Sets the second corner of a new shop region at your current position |
| `/locksmith region confirm <name>` | Creates the shop region from the two set positions |
| `/locksmith region list` | Lists all defined shop regions and the current spawn radius |
| `/locksmith region delete <name>` | Deletes a named shop region |
| `/locksmith region preview` | Shows a visual preview of the current region selection |
| `/locksmith region preview <name>` | Shows a visual preview of a saved region |
| `/locksmith region spawnradius <radius>` | Sets the default no-shop radius around world spawn (default: 100 blocks) |
