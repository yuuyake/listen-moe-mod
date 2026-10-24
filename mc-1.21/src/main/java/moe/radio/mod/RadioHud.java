package moe.radio.mod;

import moe.radio.core.HudState;
import moe.radio.core.NowPlaying;
import moe.radio.core.RadioPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

final class RadioHud {
    private static final int ART = 44;

    private RadioHud() {}

    static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("listenmoe", "now_playing"), RadioHud::render);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker tick) {
        if (HudState.editing || !HudState.visible()) return;
        Minecraft mc = Minecraft.getInstance();
        draw(g, HudState.x(mc.getWindow().getGuiScaledWidth()), HudState.y(mc.getWindow().getGuiScaledHeight()));
    }

    static void draw(GuiGraphics g, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int w = HudState.WIDTH, h = HudState.HEIGHT;
        g.fill(x, y, x + w, y + h, 0xB0181818);

        int ax = x + 8, ay = y + 8;
        if (!RadioCover.draw(g, ax, ay, ART)) g.fill(ax, ay, ax + ART, ay + ART, 0xFF000000);

        int tx = ax + ART + 10;
        String title = font.plainSubstrByWidth(HudState.title(), x + w - 8 - tx);
        g.drawString(font, title, tx, y + 10, 0xFFFFFFFF);

        boolean paused = RadioPlayer.isPaused();
        int on = 0xFFFFFFFF, off = 0xFF666666, hint = 0xFFAAAAAA;
        int c1 = tx + 14, c2 = tx + 54, c3 = tx + 116;
        centered(g, font, RadioKeys.pause(), c1, y + 25, hint);
        centered(g, font, RadioKeys.play(), c2, y + 25, hint);
        centered(g, font, RadioKeys.next(), c3, y + 25, hint);
        pauseIcon(g, c1, y + 36, paused ? on : off);
        playIcon(g, c2, y + 36, paused ? off : on);
        centered(g, font, RadioPlayer.station(), c3, y + 37, on);

        int elapsed = NowPlaying.elapsedSec(), total = NowPlaying.durationSec();
        String left = time(elapsed), right = time(total);
        int reserve = font.width("00:00");
        int ty = y + h - 14;
        g.drawString(font, left, x + 8, ty, 0xFFCCCCCC);
        g.drawString(font, right, x + w - 8 - font.width(right), ty, 0xFFCCCCCC);
        int bx0 = x + 8 + reserve + 4, bx1 = x + w - 8 - reserve - 4, by = ty + 3;
        g.fill(bx0, by, bx1, by + 2, 0x55FFFFFF);
        if (total > 0) g.fill(bx0, by, bx0 + (int) ((bx1 - bx0) * (elapsed / (double) total)), by + 2, 0xFFFFFFFF);
    }

    private static void centered(GuiGraphics g, Font f, String s, int cx, int y, int color) {
        g.drawString(f, s, cx - f.width(s) / 2, y, color);
    }

    private static void pauseIcon(GuiGraphics g, int cx, int y, int color) {
        g.fill(cx - 4, y, cx - 1, y + 10, color);
        g.fill(cx + 1, y, cx + 4, y + 10, color);
    }

    private static void playIcon(GuiGraphics g, int cx, int y, int color) {
        for (int c = 0; c < 8; c++) {
            int hh = Math.max(1, (8 - c) * 5 / 8);
            g.fill(cx - 3 + c, y + 5 - hh, cx - 2 + c, y + 5 + hh, color);
        }
    }

    private static String time(int s) {
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
