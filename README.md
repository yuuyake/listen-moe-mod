mc-1.21 and mc-26 are separate Gradle builds (run `gradle wrapper` once, then `./gradlew build`).
core/ and shared/ are compiled into both jars via srcDirs.
Commands: /radio play [kpop] | stop | np | volume <0-100>
          /radio hud | hud buttons|cover|progress [true|false] | hud scale <50-200> | hud reset
          /radio hud topleft|topright|bottomleft|bottomright
Open chat (T or /) to drag the HUD; drag its bottom-right corner to scale it.
Keys (rebind in Controls): F6 pause, F7 play/resume, F8 switch station.
Java: 1.21.x = 21, 26.x = 25.
