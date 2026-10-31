<h1 align="center">PhantomAddons</h1>

<p align="center">A Fabric QoL mod for Kuudra and general Hypixel Skyblock utilities, with a full in-game profile viewer.</p>

<div align="center">
  <a href="https://modrinth.com/mod/phantom-addons"><img alt="version" src="https://img.shields.io/modrinth/v/phantom-addons?style=for-the-badge&label=version&color=1bd96a"></a>
  &nbsp;&nbsp;
  <a href="https://discord.gg/6MquvmrXNP"><img alt="discord" src="https://img.shields.io/badge/discord-join-5865F2?style=for-the-badge&logo=discord&logoColor=white"></a>
  &nbsp;&nbsp;
  <a href="https://github.com/PhantomOrigin/Phantom-Addons/releases"><img alt="github downloads" src="https://img.shields.io/github/downloads/PhantomOrigin/Phantom-Addons/total?style=for-the-badge&label=downloads&color=208a19&logo=github"></a>
  &nbsp;&nbsp;
  <a href="https://modrinth.com/mod/phantom-addons"><img alt="modrinth downloads" src="https://img.shields.io/modrinth/dt/phantom-addons?style=for-the-badge&label=downloads&color=208a19&logo=modrinth"></a>
</div>

<br>

<div align="center">
  <a href="https://modrinth.com/mod/phantom-addons"><img alt="download on modrinth" src="https://img.shields.io/badge/download%20on-modrinth-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white"></a>
  &nbsp;&nbsp;
  <a href="https://github.com/PhantomOrigin/Phantom-Addons/releases/latest"><img alt="download on github" src="https://img.shields.io/badge/download%20on-github-24292f?style=for-the-badge&logo=github&logoColor=white"></a>
</div>

<div align="center">
  <a href="https://ko-fi.com/L4L8234BMK"><img alt="ko-fi" src="https://ko-fi.com/img/githubbutton_sm.svg"></a>
</div>

<p align="center">Supported Minecraft versions: <b>26.1.x</b>, <b>26.2</b> and <b>26.3</b></p>

---

### Features

<details>
<summary><strong>Profile Viewer</strong></summary>

- Complete SkyBlock profile information
- Networth calculator, with per-backpack and ender chest values and an optional museum value
- Per-class dungeon gear breakdowns and automatic Kuudra gear detection
- Missing and inactive accessory detection sorted by price per magic power
- Loadouts, wardrobe, equipment, pets, backpacks and ender chests
- Click on a name in chat to open that player's profile
- Icon or text tab bar, light and dark themes, adjustable window and content size
- Overflow skill levels, including Catacombs and the dungeon classes
</details>

<details>
<summary><strong>Supplies</strong></summary>

- Supply Beacons
- No Pre Announce
- No Pre Notification
- Crate Priority HUD
- Custom Supply Recovery Message
- Supply Location Announce
- Supply Start Timer
- Supply Hitbox
- Supply Pull Radius
- Supply Pearl Hitbox
- Block Skyblock Menu Clicks
- Dynamic Pearl Waypoints
- Supply Pearl Location Beacons
- Etherwarp Waypoints
- Smooth Crate Pickup HUD
- Waypoint Line
- Tentacle Grab Prediction
- Tentacle Reach Highlight
</details>

<details>
<summary><strong>Build</strong></summary>

- Build Progress Tracker
- Announce Fresh
- Fresh Notification
- Build Started Notification
- Build Beacons
- Elle Highlight
</details>

<details>
<summary><strong>Stun / DPS</strong></summary>

- Auto GFS Poisons
- Role Detection
- Incorrect Pickobulus Blocker
- Eaten Timer
- Stun Preview / Waypoint
- Cannon Auto Close
- Fast DPS Warning
- Fast DPS Notification
- Individual DPS Waypoint
</details>

<details>
<summary><strong>Boss</strong></summary>

- Kuudra Direction
- Rend Damage Tracker
- Kuudra Highlight / ESP
- Solo Announcer
- Solo Notification
- Kuudra Health Display
- Rend / Backbone Tracker
- Backbone Progress Bar
- Bonemerang Timing Assist
- Predicted Kuudra Outline
- Atomsplit Blocker
- Boss Jump Blocker
- Rend Pull Attribution (BETA)
- Right Peak Block Highlight
</details>

<details>
<summary><strong>Kuudra General</strong></summary>

- Kuudra Profit Tracker
- Chest Value HUD
- Hide Elle Dialogue
- Pearl Refill
- Middle Click Shop
- Auto Requeue
- Lava Teleport Blocker
- Hollow Wand Announcer
- Chest Tracker HUD
- Shop Keybinds
- Wardrobe Keybinds
- Loadout Keybinds
- Auto Close Loadouts
- Equipment Wardrobe Keybinds
- Explosion Hider
- Split Timer
- Moveable Crate Pickup Progress Bar
- Kuudra Mob Scale
- Shitter List with auto kick
- Tux Warning
- Ichor Radius
- Show Cannon Hitboxes
</details>

<details>
<summary><strong>Other</strong></summary>

- M7 Dragons Auto GFS Poisons
- Held Item Animation / Position / Scale Customiser
- Lava Texture Customisation (transparency, colour and replace-with-water; requires Sodium)
- Water Texture Customisation
- Auto Update
- No Selfie Cam
- Auto Sprint
- Slot Binds
- Vanilla Boss Bar Hider
- Hide Falling Blocks
- Prevent Placing Weapons
- Prevent Placing Skulls
- Player Scale Features
- Mob Fire Hider
- Visual Words
- Skyblock Kicked Cooldown and Announcer
- Block Close Items in Menus
- Legacy Rod Physics
- Lava Bobber Fix
- Fix Capitalised Commands
</details>

---

### Commands

<details>
<summary><strong>Slash commands</strong></summary>

- `/pa` : open the settings menu
- `/pv [player]` : open the profile viewer for yourself or another player
- `/pvcard kuudra|dungeons [player]` : post a Kuudra or Dungeons card in chat
- `/phantom help` : list the mod's commands
- `/phantom chests` : show the chest tracker
- `/phantom pb` : show your personal bests
- `/phantom shitter add|remove|list` : manage the Shitter List
- `/phantom tracker reset` : reset the profit tracker
</details>

<details>
<summary><strong>Party commands</strong></summary>

Type these in party chat. They can be switched on or off in the settings.

- `!w` / `!warp` : warp the party
- `!allinv` : toggle all invite
- `!inv <player>` : invite a player
- `!pt` / `!ptme` : transfer the party
- `!t1` to `!t5` : join that Kuudra tier
- `!k` / `!kick <player>` : kick a player
- `!chests` : post the chest counter
- `!avg` / `!average` : post your average Kuudra run time for your highest tier, with the run count. With no option it uses the current party.
  - `!avg party` : the average for the current party's runs (the default)
  - `!avg session` : the average for this play session
  - `!avg total` : the all-time average
- `!pb` : personal bests
- `!dt` / `!undt` : downtime announcements
</details>

---

### Instructions

1. Download the mod from [Modrinth](https://modrinth.com/mod/phantom-addons) or [GitHub](https://github.com/PhantomOrigin/Phantom-Addons/releases/latest).
2. Put the `.jar` in your `mods` folder. You'll need [Fabric Loader](https://fabricmc.net/use/), the [Fabric API](https://modrinth.com/mod/fabric-api) and [Sodium](https://modrinth.com/mod/sodium). The lava transparency, colour and "replace with water" effects are built on Sodium's fluid renderer.
3. Launch the game and type `/pa` to open the settings.

<details>
<summary><strong>Editions and building from source</strong></summary>

| Edition | Command | Notes |
|---|---|---|
| Full | `./gradlew build -Pedition=full` | All features, with the in-game update download option |
| Full, no auto update | `./gradlew build -Pedition=noauto` | The full version without the in-game download option |
| Standard (default) | `./gradlew build -Pedition=standard` | No Auto GFS features and no auto close features |

`build.gradle.kts` leaves `UpdateInstaller.java` out of the standard and no-auto-update builds entirely. The auto close and auto GFS features are only switched off in the standard version, not removed.
</details>

---

### Links

- [Modrinth](https://modrinth.com/mod/phantom-addons)
- [GitHub releases](https://github.com/PhantomOrigin/Phantom-Addons/releases)
- [Discord](https://discord.gg/6MquvmrXNP)
- [Privacy](https://phantomaddonsapi.com/privacy)
- [Support on Ko-fi](https://ko-fi.com/L4L8234BMK)

### Credits

- Auction house prices provided by [SkyCofl](https://sky.coflnet.com)
