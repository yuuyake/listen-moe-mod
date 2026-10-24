package moe.radio.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HudState {
    public enum Corner {
        TOP_LEFT("topleft"), TOP_RIGHT("topright"), BOTTOM_LEFT("bottomleft"), BOTTOM_RIGHT("bottomright");
        public final String arg;
        Corner(String arg) { this.arg = arg; }
    }

    public static volatile boolean enabled = true;
    public static volatile Corner corner = Corner.TOP_LEFT;

    private HudState() {}

    /** Text for the HUD, or null if nothing should be drawn. */
    public static String line() {
        if (!enabled || !RadioPlayer.isPlaying()) return null;
        return "\u266A " + NowPlaying.get();
    }

    public static void load(Path file) {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("hud", "true"));
            try { corner = Corner.valueOf(p.getProperty("corner", "TOP_LEFT")); } catch (IllegalArgumentException ignored) {}
            try { RadioPlayer.setVolume(Integer.parseInt(p.getProperty("volume", "50"))); } catch (NumberFormatException ignored) {}
        } catch (Exception ignored) {}
    }

    public static void save(Path file) {
        Properties p = new Properties();
        p.setProperty("hud", String.valueOf(enabled));
        p.setProperty("corner", corner.name());
        p.setProperty("volume", String.valueOf(RadioPlayer.getVolume()));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "ListenMoe Radio");
        } catch (Exception ignored) {}
    }
}
