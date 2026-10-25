# Modrinth page

## Project settings
- Name: ListenMoe Radio (check the name is free on Modrinth first)
- Slug: listenmoe-radio
- Summary (max 256 chars): Stream LISTEN.moe J-POP and K-POP radio in-game with a movable player HUD: cover art, now playing, progress bar, synced lyrics and keybinds to pause or switch stations. Client-side only.
- Categories: Utility
- Client side: Required. Server side: Unsupported
- License: MIT (change it in fabric.mod.json if you pick another)
- Source: https://github.com/yuuyake/listen-moe-mod
- Issues: https://github.com/yuuyake/listen-moe-mod/issues
- Icon: modrinth/icon.png (placeholder, swap in your own if you like)
- Gallery: 3 screenshots. Full HUD with cover and lyrics, the chat-mode drag and scale outline, and a minimal HUD with buttons hidden.

## Description (paste into the Modrinth description box)

# ListenMoe Radio

Listen to [LISTEN.moe](https://listen.moe) (J-POP and K-POP) while you play. Client-side only, works on any server.

## Features
- Plays the live stream in-game, no browser needed
- Player HUD with cover art, title and artist, progress bar and station name
- Synced lyrics line under the player when lyrics are available
- Open chat (T or /) and drag the HUD anywhere. Drag the bottom-right corner to scale it
- Hide any part you do not want: cover, buttons, progress, lyrics
- Keybinds for pause, play and switching station, rebindable in Controls
- Position, scale, volume and options are saved

## Commands
| Command | What it does |
|---|---|
| `/radio play [kpop]` | Start the radio |
| `/radio stop` | Stop and hide the HUD |
| `/radio volume <0-100>` | Set volume |
| `/radio np` | Show the current song |
| `/radio hud` | Toggle the HUD |
| `/radio hud buttons\|cover\|progress\|lyrics [true\|false]` | Show or hide a part |
| `/radio hud scale <50-200>` | Set the size in percent |
| `/radio hud topleft\|topright\|bottomleft\|bottomright` | Snap to a corner |
| `/radio hud reset` | Reset the HUD |
| `/radio lyrics offset <ms>` | Shift lyrics timing. Negative values show lyrics later |

## Keybinds
F6 pause, F7 play or resume, F8 switch between J-POP and K-POP.

## Good to know
- It is a live stream, so pause drops the stream and play rejoins live.
- The progress bar and lyrics use the song start time from LISTEN.moe and can run a few seconds ahead of the audio. Use `/radio lyrics offset -3000` to line them up.
- Lyrics come from LRCLIB. Anime songs are often missing there, and then the lyrics line hides itself.
- The cover hides on its own when a song has none and returns for the next song that does.

## Requirements
Fabric Loader and Fabric API.

## Credits and legal
Unofficial, not affiliated with LISTEN.moe. Music and metadata come from LISTEN.moe, lyrics from LRCLIB. No lyrics or audio are bundled in the mod.
Bundled libraries: JLayer (LGPL), Java-WebSocket (MIT).

## Versions to upload
One file per Minecraft version. Loader: Fabric. Dependency: Fabric API (required).

| File | Game versions |
|---|---|
| listenmoe-radio-mc1.20.1-1.0.0.jar | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4 |
| listenmoe-radio-mc1.21.1-1.0.0.jar | 1.21, 1.21.1 |
| listenmoe-radio-mc1.21.11-1.0.0.jar | 1.21.11 |
| listenmoe-radio-mc26.1-1.0.0.jar | 26.1 and later 26.1.x |

Only list versions you tested. Use the Beta release channel until you have run each jar on a real client.

## Changelog template
```
1.0.0
- First release
- Live J-POP and K-POP radio, player HUD, synced lyrics, keybinds
```
