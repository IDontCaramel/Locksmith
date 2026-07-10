# SafeSale

SafeSale is a Fabric mod for Minecraft 1.20.1 that adds simple vanilla style player shops.

Player can create/manage thier own shops and admins can also acces them via commands if that is nececary

You can create a shop using the `/safesale sell 10 minecraft:diamond` or by renaming a chest in an anvil `10 diamond`

Admins can also set regions where shops can be placed and there is also a default region around the world spawn of 100 blocks

## Versions

- Minecraft: 1.20.1
- Fabric Loader: 0.19.3+
- Java: 17+

## Build

```bash
./gradlew build
```

## Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/safesale sell <amount> <item>` | Prepares the chest in your hand as a shop sell chest |
| Rename a chest in anvil to `<amount> <item>` | Alternative way to create a shop chest (e.g. `10 diamond`) |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/safesale admin` | Toggles admin shop access mode — allows opening any shop chest |
| `/safesale region set pos1` | Sets the first corner of a new shop region at your current position |
| `/safesale region set pos2` | Sets the second corner of a new shop region at your current position |
| `/safesale region confirm <name>` | Creates the shop region from the two set positions |
| `/safesale region list` | Lists all defined shop regions and the current spawn radius |
| `/safesale region delete <name>` | Deletes a named shop region |
| `/safesale region preview` | Shows a visual preview of the current region selection |
| `/safesale region preview <name>` | Shows a visual preview of a saved region |
| `/safesale region spawnradius <radius>` | Sets the default no-shop radius around world spawn (default: 100 blocks) |
