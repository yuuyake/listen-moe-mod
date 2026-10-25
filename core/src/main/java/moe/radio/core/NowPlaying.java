package moe.radio.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public final class NowPlaying {
    public static final int COVER_SIZE = 64;
    public static final int COVER_NONE = 0, COVER_LOADING = 1, COVER_READY = 2;

    public static final class Track {
        public final String title, artist;
        public final long startMs;
        public final boolean startKnown;
        public final int durationSec;
        Track(String title, String artist, long startMs, boolean startKnown, int durationSec) {
            this.title = title; this.artist = artist; this.startMs = startMs;
            this.startKnown = startKnown; this.durationSec = durationSec;
        }
    }

    private static volatile Track track;
    private static volatile int[] coverPixels;
    private static volatile int coverVersion;
    private static volatile int coverState = COVER_LOADING;
    private static volatile boolean coverInit;
    private static volatile int seq;
    private static volatile String lastCoverUrl;
    private static volatile long clockOffset;
    private static volatile String lastRaw = "";
    private static WebSocketClient ws;
    private static Timer timer;

    private NowPlaying() {}

    public static String get() {
        Track t = track;
        if (t == null) return "Loading...";
        return t.artist.isEmpty() ? t.title : t.title + " - " + t.artist;
    }

    public static int durationSec() { Track t = track; return t == null ? 0 : t.durationSec; }

    static void calibrate(long offset) { clockOffset = offset; }

    private static long now() { return System.currentTimeMillis() + clockOffset; }

    public static long elapsedMs() {
        Track t = track;
        if (t == null) return 0;
        long ms = now() - t.startMs;
        if (ms < 0) ms = 0;
        if (t.durationSec > 0 && ms > t.durationSec * 1000L) ms = t.durationSec * 1000L;
        return ms;
    }

    public static int elapsedSec() { return (int) (elapsedMs() / 1000); }

    public static String describe() {
        Track t = track;
        if (t == null) return "Loading...";
        StringBuilder sb = new StringBuilder(get());
        sb.append(" (").append(fmt(elapsedSec())).append(" / ").append(fmt(t.durationSec)).append(")");
        if (!t.startKnown) sb.append(" [no usable start time]");
        if (t.durationSec <= 0) sb.append(" [no duration]");
        return sb.toString();
    }

    public static String lastRaw() {
        String r = lastRaw;
        if (r.isEmpty()) return "no track message received yet";
        return r.length() > 400 ? r.substring(0, 400) : r;
    }

    private static String fmt(int s) { return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60); }

    public static int coverVersion() { return coverVersion; }
    public static int coverState() { return coverState; }
    public static int[] coverPixels() { return coverPixels; }

    static synchronized void connect(boolean kpop) {
        disconnect();
        track = null;
        lastCoverUrl = null;
        coverInit = false;
        Lyrics.clear();
        setCover(null, ++seq, COVER_LOADING);
        try {
            URI uri = new URI(kpop ? "wss://listen.moe/kpop/gateway_v2" : "wss://listen.moe/gateway_v2");
            ws = new WebSocketClient(uri) {
                @Override public void onOpen(ServerHandshake h) {}
                @Override public void onMessage(String m) { handle(m); }
                @Override public void onClose(int c, String r, boolean remote) {}
                @Override public void onError(Exception e) {}
            };
            ws.connect();
        } catch (Exception ignored) {}
    }

    static synchronized void disconnect() {
        if (timer != null) { timer.cancel(); timer = null; }
        if (ws != null) { try { ws.close(); } catch (Exception ignored) {} ws = null; }
    }

    private static void handle(String msg) {
        try {
            JsonObject o = new JsonParser().parse(msg).getAsJsonObject();
            int op = o.get("op").getAsInt();
            if (op == 0) {
                final long hb = o.getAsJsonObject("d").get("heartbeat").getAsLong();
                synchronized (NowPlaying.class) {
                    if (timer != null) timer.cancel();
                    timer = new Timer("listenmoe-hb", true);
                    timer.scheduleAtFixedRate(new TimerTask() {
                        public void run() { WebSocketClient w = ws; if (w != null && w.isOpen()) w.send("{\"op\":9}"); }
                    }, 0, hb);
                }
            } else if (op == 1 && o.has("d") && o.get("d").isJsonObject() && o.getAsJsonObject("d").has("song")) {
                lastRaw = msg;
                JsonObject d = o.getAsJsonObject("d");
                JsonObject song = d.getAsJsonObject("song");
                String title = str(song, "title");
                String firstArtist = null;
                StringBuilder sb = new StringBuilder();
                JsonElement ar = song.get("artists");
                if (ar != null && ar.isJsonArray()) {
                    for (JsonElement a : (JsonArray) ar) {
                        String n = str(a.getAsJsonObject(), "name");
                        if (n == null) continue;
                        if (firstArtist == null) firstArtist = n;
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(n);
                    }
                }
                int dur = 0;
                JsonElement de = song.get("duration");
                if (de != null && !de.isJsonNull()) {
                    try { dur = (int) Math.round(Double.parseDouble(de.getAsString())); } catch (Exception ignored) {}
                }
                long arrival = now();
                long start = 0;
                boolean known = false;
                String st = str(d, "startTime");
                if (st != null) {
                    try { start = OffsetDateTime.parse(st).toInstant().toEpochMilli(); known = true; } catch (Exception ignored) {}
                }
                if (known && dur > 0) {
                    long el = arrival - start;
                    if (el < -3000 || el > dur * 1000L + 3000) known = false;
                }
                if (!known) start = arrival;
                Track prev = track;
                Track nt = new Track(title == null ? "unknown" : title, sb.toString(), start, known, dur);
                track = nt;
                if (prev == null || !prev.title.equals(nt.title) || !prev.artist.equals(nt.artist)) {
                    Lyrics.load(firstArtist == null ? "" : firstArtist, nt.title, firstName(song, "albums"), dur);
                }

                String cover = coverUrl(song);
                boolean changed = !coverInit || (cover == null ? lastCoverUrl != null : !cover.equals(lastCoverUrl));
                if (changed) {
                    coverInit = true;
                    lastCoverUrl = cover;
                    loadCover(cover, ++seq);
                }
            }
        } catch (Exception ignored) {}
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String firstImage(JsonObject song, String key) {
        JsonElement arr = song.get(key);
        if (arr == null || !arr.isJsonArray()) return null;
        for (JsonElement e : (JsonArray) arr) {
            if (!e.isJsonObject()) continue;
            String img = str(e.getAsJsonObject(), "image");
            if (img != null && !img.isEmpty()) return img;
        }
        return null;
    }

    private static String firstName(JsonObject song, String key) {
        JsonElement arr = song.get(key);
        if (arr == null || !arr.isJsonArray()) return null;
        for (JsonElement e : (JsonArray) arr) {
            if (!e.isJsonObject()) continue;
            String n = str(e.getAsJsonObject(), "name");
            if (n != null && !n.isEmpty()) return n;
        }
        return null;
    }

    private static String coverUrl(JsonObject song) {
        String img = firstImage(song, "albums");
        if (img != null) return "https://cdn.listen.moe/covers/" + img;
        img = firstImage(song, "artists");
        if (img != null) return "https://cdn.listen.moe/artists/" + img;
        return null;
    }

    private static void loadCover(final String url, final int my) {
        if (url == null) { setCover(null, my, COVER_NONE); return; }
        setCover(null, my, COVER_LOADING);
        Thread t = new Thread(new Runnable() {
            public void run() {
                int[] px = null;
                try { px = fetch(url); } catch (Exception ignored) {}
                setCover(px, my, px != null ? COVER_READY : COVER_NONE);
            }
        }, "listenmoe-cover");
        t.setDaemon(true);
        t.start();
    }

    private static synchronized void setCover(int[] px, int my, int state) {
        if (my != seq) return;
        coverPixels = px;
        coverState = state;
        coverVersion++;
    }

    private static int[] fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "listenmoe-fabric/1.0");
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        BufferedImage src;
        try (InputStream in = c.getInputStream()) { src = ImageIO.read(in); }
        if (src == null) return null;
        int S = COVER_SIZE, w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h), sx = (w - side) / 2, sy = (h - side) / 2;
        int[] out = new int[S * S];
        for (int dy = 0; dy < S; dy++) {
            int y0 = sy + dy * side / S, y1 = Math.max(y0 + 1, sy + (dy + 1) * side / S);
            for (int dx = 0; dx < S; dx++) {
                int x0 = sx + dx * side / S, x1 = Math.max(x0 + 1, sx + (dx + 1) * side / S);
                long r = 0, g = 0, b = 0; int n = 0;
                for (int yy = y0; yy < y1 && yy < h; yy++) {
                    for (int xx = x0; xx < x1 && xx < w; xx++) {
                        int p = src.getRGB(xx, yy);
                        r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
                    }
                }
                if (n == 0) n = 1;
                out[dy * S + dx] = 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
            }
        }
        return out;
    }
}
