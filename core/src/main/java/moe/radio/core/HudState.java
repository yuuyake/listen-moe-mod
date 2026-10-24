package moe.radio.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HudState {
    public static final int WIDTH = 230, HEIGHT = 72;
    private static final int M = 4;

    public enum Corner {
        TOP_LEFT("topleft", 0, 0), TOP_RIGHT("topright", 1, 0),
        BOTTOM_LEFT("bottomleft", 0, 1), BOTTOM_RIGHT("bottomright", 1, 1);
        public final String arg; public final double fx, fy;
        Corner(String arg, double fx, double fy) { this.arg = arg; this.fx = fx; this.fy = fy; }
    }

    public static volatile boolean enabled = true;
    public static volatile boolean editing = false;
    public static volatile double fx = 0, fy = 0;

    private HudState() {}

    public static boolean visible() {
        return enabled && (RadioPlayer.isPlaying() || RadioPlayer.isPaused());
    }

    public static String title() {
        return (RadioPlayer.isPlaying() || RadioPlayer.isPaused()) ? NowPlaying.get() : "Name - Artist";
    }

    public static int x(int sw) { return M + (int) Math.round(fx * Math.max(0, sw - WIDTH - 2 * M)); }
    public static int y(int sh) { return M + (int) Math.round(fy * Math.max(0, sh - HEIGHT - 2 * M)); }

    public static void setPos(int px, int py, int sw, int sh) {
        fx = clamp((px - M) / (double) Math.max(1, sw - WIDTH - 2 * M));
        fy = clamp((py - M) / (double) Math.max(1, sh - HEIGHT - 2 * M));
    }

    public static void setCorner(Corner c) { fx = c.fx; fy = c.fy; }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }

    public static void load(Path file) {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("hud", "true"));
            try { fx = clamp(Double.parseDouble(p.getProperty("x", "0"))); } catch (NumberFormatException ignored) {}
            try { fy = clamp(Double.parseDouble(p.getProperty("y", "0"))); } catch (NumberFormatException ignored) {}
            try { RadioPlayer.setVolume(Integer.parseInt(p.getProperty("volume", "50"))); } catch (NumberFormatException ignored) {}
        } catch (Exception ignored) {}
    }

    public static void save(Path file) {
        Properties p = new Properties();
        p.setProperty("hud", String.valueOf(enabled));
        p.setProperty("x", String.valueOf(fx));
        p.setProperty("y", String.valueOf(fy));
        p.setProperty("volume", String.valueOf(RadioPlayer.getVolume()));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "ListenMoe Radio");
        } catch (Exception ignored) {}
    }
}
