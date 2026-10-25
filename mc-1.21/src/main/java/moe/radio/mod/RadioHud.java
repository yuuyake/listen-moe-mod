package moe.radio.mod;

import moe.radio.core.HudState;
import moe.radio.core.NowPlaying;
import moe.radio.core.RadioPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class RadioHud {
    private static final int ART = 44;
    private static boolean wasDown, dragging, resizing;
    private static int offX, offY;

    private RadioHud() {}

    static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("listenmoe", "now_playing"), RadioHud::render);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker tick) {
        Minecraft mc = Minecraft.getInstance();
        boolean chat = mc.screen instanceof ChatScreen;
        if (!chat) {
            dragging = false;
            resizing = false;
            wasDown = false;
        }
        if (!HudState.enabled || !(chat || HudState.visible())) return;
        var win = mc.getWindow();
        int sw = win.getGuiScaledWidth(), sh = win.getGuiScaledHeight();
        if (chat) interact(mc, win, sw, sh);
        int x = HudState.x(sw), y = HudState.y(sh);
        draw(g, x, y);
        if (chat) decorate(g, mc.font, x, y);
    }

    private static void interact(Minecraft mc, com.mojang.blaze3d.platform.Window win, int sw, int sh) {
        double mx = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / (double) win.getScreenWidth();
        double my = mc.mouseHandler.ypos() * win.getGuiScaledHeight() / (double) win.getScreenHeight();
        int x = HudState.x(sw), y = HudState.y(sh), w = HudState.scaledW(), h = HudState.scaledH();
        long handle = GLFW.glfwGetCurrentContext();
        boolean down = handle != 0
            ? GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            : mc.mouseHandler.isLeftPressed();
        if (down && !wasDown) {
            if (mx >= x + w - 10 && mx < x + w && my >= y + h - 10 && my < y + h) {
                resizing = true;
            } else if (mx >= x && mx < x + w && my >= y && my < y + h) {
                dragging = true;
                offX = (int) mx - x;
                offY = (int) my - y;
            }
        }
        if (!down && (dragging || resizing)) {
            dragging = false;
            resizing = false;
            HudState.save(RadioMod.cfg());
        }
        wasDown = down;
        if (dragging) HudState.setPos((int) mx - offX, (int) my - offY, sw, sh);
        else if (resizing) HudState.setScale((mx - x) / HudState.WIDTH, sw, sh);
    }

    private static void decorate(GuiGraphics g, Font font, int x, int y) {
        int w = HudState.scaledW(), h = HudState.scaledH();
        int c = (dragging || resizing) ? 0xFFFFFFFF : 0xFF8888FF;
        g.fill(x - 1, y - 1, x + w + 1, y, c);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, c);
        g.fill(x - 1, y, x, y + h, c);
        g.fill(x + w, y, x + w + 1, y + h, c);
        g.fill(x + w - 8, y + h - 8, x + w, y + h, c);
        g.drawString(font, "drag to move, corner to scale", x, y >= 12 ? y - 11 : y + h + 3, 0xFFCCCCCC);
    }

    static void draw(GuiGraphics g, int x, int y) {
        float s = (float) HudState.scale;
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(s, s);
        body(g);
        g.pose().popMatrix();
    }

    private static void body(GuiGraphics g) {
        Font font = Minecraft.getInstance().font;
        int w = HudState.WIDTH, h = HudState.height();
        g.fill(0, 0, w, h, 0xB0181818);

        int tx = 8;
        if (HudState.coverVisible()) {
            if (!RadioCover.draw(g, 8, 8, ART)) g.fill(8, 8, 8 + ART, 8 + ART, 0xFF000000);
            tx = 8 + ART + 10;
        }
        String title = font.plainSubstrByWidth(HudState.title(), w - 8 - tx);
        g.drawString(font, title, tx, 10, 0xFFFFFFFF);

        if (HudState.showButtons) {
            boolean paused = RadioPlayer.isPaused();
            int on = 0xFFFFFFFF, off = 0xFF666666, hint = 0xFFAAAAAA;
            int c1 = tx + 14, c2 = tx + 54, c3 = tx + 116;
            centered(g, font, RadioKeys.pause(), c1, 25, hint);
            centered(g, font, RadioKeys.play(), c2, 25, hint);
            centered(g, font, RadioKeys.next(), c3, 25, hint);
            pauseIcon(g, c1, 36, paused ? on : off);
            playIcon(g, c2, 36, paused ? off : on);
            centered(g, font, RadioPlayer.station(), c3, 37, on);
        }

        if (HudState.showProgress) {
            long ms = NowPlaying.elapsedMs();
            int total = NowPlaying.durationSec();
            String left = time((int) (ms / 1000)), right = time(total);
            int reserve = font.width("00:00");
            int ty = HudState.progressY();
            g.drawString(font, left, 8, ty, 0xFFCCCCCC);
            g.drawString(font, right, w - 8 - font.width(right), ty, 0xFFCCCCCC);
            int bx0 = 8 + reserve + 4, bx1 = w - 8 - reserve - 4, by = ty + 3;
            g.fill(bx0, by, bx1, by + 2, 0x55FFFFFF);
            if (total > 0) {
                double frac = Math.min(1.0, ms / (total * 1000.0));
                g.fill(bx0, by, bx0 + (int) ((bx1 - bx0) * frac), by + 2, 0xFFFFFFFF);
            }
        }

        if (HudState.lyricsVisible()) {
            String ly = font.plainSubstrByWidth(HudState.lyricsText(), w - 16);
            centered(g, font, ly, w / 2, HudState.lyricsY(), 0xFFFFD6EA);
        }
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
