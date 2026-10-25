package moe.radio.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Lyrics {
    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
    private static final String BASE = "https://lrclib.net/api/";
    private static final String UA = "listenmoe-fabric/1.0 (https://github.com/yuuyake/listen-moe-mod)";

    private static final class Line {
        final long ms;
        final String text;
        Line(long ms, String text) { this.ms = ms; this.text = text; }
    }

    private static volatile List<Line> lines;
    private static int seq;

    private Lyrics() {}

    static synchronized void clear() {
        seq++;
        lines = null;
    }

    static void load(final String artist, final String title, final String album, final int durationSec) {
        final int my;
        synchronized (Lyrics.class) {
            my = ++seq;
            lines = null;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                List<Line> r = null;
                try { r = fetch(artist, title, album, durationSec); } catch (Exception ignored) {}
                synchronized (Lyrics.class) { if (my == seq) lines = r; }
            }
        }, "listenmoe-lyrics");
        t.setDaemon(true);
        t.start();
    }

    public static boolean available() {
        List<Line> l = lines;
        return l != null && !l.isEmpty();
    }

    public static String line(long elapsedMs) {
        List<Line> l = lines;
        if (l == null || l.isEmpty()) return "";
        long t = elapsedMs + HudState.lyricsOffsetMs;
        String cur = "";
        for (Line x : l) {
            if (x.ms > t) break;
            cur = x.text;
        }
        return cur;
    }

    private static List<Line> fetch(String artist, String title, String album, int dur) throws Exception {
        if (title == null || title.isEmpty()) return null;
        if (!artist.isEmpty()) {
            String q = "artist_name=" + enc(artist) + "&track_name=" + enc(title);
            if (album != null && !album.isEmpty()) q += "&album_name=" + enc(album);
            if (dur > 0) q += "&duration=" + dur;
            String body = get(BASE + "get?" + q);
            if (body != null) {
                List<Line> r = parse(synced(new JsonParser().parse(body).getAsJsonObject()));
                if (r != null && !r.isEmpty()) return r;
            }
        }
        String sq = "track_name=" + enc(title) + (artist.isEmpty() ? "" : "&artist_name=" + enc(artist));
        String s = get(BASE + "search?" + sq);
        if (s == null) return null;
        JsonArray arr = new JsonParser().parse(s).getAsJsonArray();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String sl = synced(o);
            if (sl == null) continue;
            JsonElement de = o.get("duration");
            if (dur > 0 && de != null && !de.isJsonNull() && Math.abs(de.getAsDouble() - dur) > 5) continue;
            List<Line> p = parse(sl);
            if (p != null && !p.isEmpty()) return p;
        }
        return null;
    }

    private static String synced(JsonObject o) {
        JsonElement e = o.get("syncedLyrics");
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        if (c.getResponseCode() != 200) return null;
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static List<Line> parse(String lrc) {
        if (lrc == null || lrc.isEmpty()) return null;
        List<Line> out = new ArrayList<Line>();
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher m = TS.matcher(raw);
            List<Long> times = new ArrayList<Long>();
            int end = 0;
            while (m.find() && m.start() == end) {
                times.add(Integer.parseInt(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2)) * 1000));
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (Long ms : times) out.add(new Line(ms, text));
        }
        Collections.sort(out, new Comparator<Line>() {
            public int compare(Line a, Line b) { return Long.compare(a.ms, b.ms); }
        });
        return out;
    }
}
