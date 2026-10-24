package moe.radio.core;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/** Streams MP3 from listen.moe and plays it locally. No Minecraft dependencies. */
public final class RadioPlayer {
    public static final String JPOP = "https://listen.moe/fallback";
    public static final String KPOP = "https://listen.moe/kpop/fallback";

    private static volatile boolean running;
    private static volatile float volume = 0.5f;
    private static volatile SourceDataLine line;
    private static volatile int lineRate, lineCh;

    private RadioPlayer() {}

    public static synchronized void play(final String url) {
        stop();
        running = true;
        Thread t = new Thread(new Runnable() { public void run() { loop(url); } }, "listenmoe-audio");
        t.setDaemon(true);
        t.start();
        NowPlaying.connect(url.contains("kpop"));
    }

    public static synchronized void stop() {
        running = false;
        NowPlaying.disconnect();
        closeLine();
    }

    public static boolean isPlaying() { return running; }
    public static int getVolume() { return Math.round(volume * 100); }
    public static void setVolume(int percent) { volume = Math.max(0, Math.min(100, percent)) / 100f; }

    private static void loop(String url) {
        while (running) {
            try (InputStream in = open(url)) {
                decode(in);
            } catch (Exception e) {
                if (!running) break;
                try { Thread.sleep(3000); } catch (InterruptedException ie) { break; }
            }
        }
        closeLine();
    }

    private static InputStream open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "listenmoe-fabric/1.0");
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        return new BufferedInputStream(c.getInputStream(), 65536);
    }

    private static void decode(InputStream in) throws Exception {
        Bitstream bs = new Bitstream(in);
        Decoder dec = new Decoder();
        while (running) {
            Header h = bs.readFrame();
            if (h == null) break;
            SampleBuffer sb = (SampleBuffer) dec.decodeFrame(h, bs);
            ensureLine(sb.getSampleFrequency(), sb.getChannelCount());
            short[] s = sb.getBuffer();
            int n = sb.getBufferLength();
            byte[] out = new byte[n * 2];
            float v = volume;
            for (int i = 0; i < n; i++) {
                int x = (int) (s[i] * v);
                out[i * 2] = (byte) x;
                out[i * 2 + 1] = (byte) (x >> 8);
            }
            SourceDataLine l = line;
            if (l == null) break;
            l.write(out, 0, out.length);
            bs.closeFrame();
        }
    }

    private static synchronized void ensureLine(int rate, int ch) throws Exception {
        if (line != null && lineRate == rate && lineCh == ch) return;
        closeLine();
        AudioFormat fmt = new AudioFormat(rate, 16, ch, true, false);
        SourceDataLine l = AudioSystem.getSourceDataLine(fmt);
        l.open(fmt, rate * ch * 2); // ~0.5s buffer
        l.start();
        line = l; lineRate = rate; lineCh = ch;
    }

    private static synchronized void closeLine() {
        SourceDataLine l = line;
        line = null;
        if (l != null) { try { l.stop(); l.close(); } catch (Exception ignored) {} }
    }
}
