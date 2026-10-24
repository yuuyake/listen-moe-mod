package moe.radio.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/** Tracks the current song playing. */
public final class NowPlaying {
    private static volatile String current = "unknown";
    private static WebSocketClient ws;
    private static Timer timer;

    private NowPlaying() {}

    public static String get() { return current; }

    static synchronized void connect(boolean kpop) {
        disconnect();
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
            } else if (op == 1 && o.has("d") && o.getAsJsonObject("d").has("song")) {
                JsonObject song = o.getAsJsonObject("d").getAsJsonObject("song");
                String title = song.get("title").getAsString();
                StringBuilder sb = new StringBuilder();
                JsonElement ar = song.get("artists");
                if (ar != null && ar.isJsonArray()) {
                    for (JsonElement a : (JsonArray) ar) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(a.getAsJsonObject().get("name").getAsString());
                    }
                }
                current = sb.length() > 0 ? sb + " - " + title : title;
            }
        } catch (Exception ignored) {}
    }
}
