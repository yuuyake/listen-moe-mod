Each mc-* folder is its own Gradle build: run `gradle wrapper` once inside it, then `./gradlew build`.
core/ and shared/ are compiled into every jar. Jars land in mc-*/build/libs (use the one without -sources or -dev).

Folders and Java: mc-1.20 (1.20.1, Java 17), mc-1.21.1 (1.21 to 1.21.1, Java 21), mc-1.21 (1.21.11, Java 21), mc-26 (26.1, Java 25).
Building any of them with JDK 21 works except mc-26, which needs JDK 25.

Fabric API versions in build.gradle are examples. To get the newest for a Minecraft version:
curl -s 'https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%221.20.1%22%5D&loaders=%5B%22fabric%22%5D' | grep -o '"version_number":"[^"]*"' | head -1

Commands: /radio play [kpop] | stop | np | raw | volume <0-100>
          /radio hud | hud buttons|cover|progress|lyrics [true|false] | hud scale <50-200> | hud reset
          /radio hud topleft|topright|bottomleft|bottomright
          /radio lyrics offset <ms>   (negative shows lyrics later)
Open chat (T or /) to drag the HUD; drag its bottom-right corner to scale it.
Keys (rebind in Controls): F6 pause, F7 play/resume, F8 switch station.
Modrinth page text and upload checklist: MODRINTH.md
