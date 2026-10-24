package moe.radio.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HudState {
    public static final int WIDTH = 230;
    private static final int M = 4;

    public enum Corner {
        TOP_LEFT("topleft", 0, 0), TOP_RIGHT("topright", 1, 0),
        BOTTOM_LEFT("bottomleft", 0, 1), BOTTOM_RIGHT("bottomright", 1, 1);
        public final String arg; public final double fx, fy;
        Corner(String arg, double fx, double fy) { this.arg = arg; this.fx = fx; this.fy = fy; }
    }

    public static volatile boolean enabled = true;
    public static volatile boolean showButtons = true;
    public static volatile boolean showCover = true;
    public static volatile boolean showProgress = true;
    public static volatile double fx = 0, fy = 0;
    public static volatile double scale = 1.0;

    private HudState() {}

    public static boolean visible() {
        return enabled && (RadioPlayer.isPlaying() || RadioPlayer.isPaused());
    }

    public static String title() {
        return (RadioPlayer.isPlaying() || RadioPlayer.isPaused()) ? NowPlaying.get() : "Name - Artist";
    }

    public static int height() {
        int body = Math.max(showCover ? 44 : 0, showButtons ? 38 : 10);
        return 8 + body + (showProgress ? 20 : 8);
    }

    public static int scaledW() { return (int) Math.round(WIDTH * scale); }
    public static int scaledH() { return (int) Math.round(height() * scale); }

    public static int x(int sw) { return M + (int) Math.round(fx * Math.max(0, sw - scaledW() - 2 * M)); }
    public static int y(int sh) { return M + (int) Math.round(fy * Math.max(0, sh - scaledH() - 2 * M)); }

    public static void setPos(int px, int py, int sw, int sh) {
        fx = clamp((px - M) / (double) Math.max(1, sw - scaledW() - 2 * M));
        fy = clamp((py - M) / (double) Math.max(1, sh - scaledH() - 2 * M));
    }

    public static void setScale(double s, int sw, int sh) {
        int px = x(sw), py = y(sh);
        scale = Math.max(0.5, Math.min(2.0, s));
        setPos(px, py, sw, sh);
    }

    public static void setCorner(Corner c) { fx = c.fx; fy = c.fy; }

    public static void reset() {
        fx = 0; fy = 0; scale = 1.0;
        enabled = true; showButtons = true; showCover = true; showProgress = true;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }

    private static double num(Properties p, String k, double def) {
        try { return Double.parseDouble(p.getProperty(k, String.valueOf(def))); } catch (NumberFormatException e) { return def; }
    }

    public static void load(Path file) {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties p = new Properties();
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("hud", "true"));
            showButtons = Boolean.parseBoolean(p.getProperty("buttons", "true"));
            showCover = Boolean.parseBoolean(p.getProperty("cover", "true"));
            showProgress = Boolean.parseBoolean(p.getProperty("progress", "true"));
            fx = clamp(num(p, "x", 0));
            fy = clamp(num(p, "y", 0));
            scale = Math.max(0.5, Math.min(2.0, num(p, "scale", 1.0)));
            RadioPlayer.setVolume((int) num(p, "volume", 50));
        } catch (Exception ignored) {}
    }

    public static void save(Path file) {
        Properties p = new Properties();
        p.setProperty("hud", String.valueOf(enabled));
        p.setProperty("buttons", String.valueOf(showButtons));
        p.setProperty("cover", String.valueOf(showCover));
        p.setProperty("progress", String.valueOf(showProgress));
        p.setProperty("x", String.valueOf(fx));
        p.setProperty("y", String.valueOf(fy));
        p.setProperty("scale", String.valueOf(scale));
        p.setProperty("volume", String.valueOf(RadioPlayer.getVolume()));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "ListenMoe Radio");
        } catch (Exception ignored) {}
    }
}
