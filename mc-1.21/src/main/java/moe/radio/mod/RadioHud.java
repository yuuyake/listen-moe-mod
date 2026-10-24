package moe.radio.mod;

import moe.radio.core.HudState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**Toggle with /radio hud */
final class RadioHud {
    private RadioHud() {}

    static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("listenmoe", "now_playing"), RadioHud::render);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker tick) {
        String line = HudState.line();
        if (line == null) return;
        Font font = Minecraft.getInstance().font;
        line = font.plainSubstrByWidth(line, 240);
        int w = font.width(line) + 8;
        int h = font.lineHeight + 6;
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        HudState.Corner c = HudState.corner;
        int x = (c == HudState.Corner.TOP_RIGHT || c == HudState.Corner.BOTTOM_RIGHT) ? sw - w - 4 : 4;
        int y = (c == HudState.Corner.BOTTOM_LEFT || c == HudState.Corner.BOTTOM_RIGHT) ? sh - h - 4 : 4;
        g.fill(x, y, x + w, y + h, 0x90000000);
        g.drawString(font, line, x + 4, y + 3, 0xFFFFFFFF);
    }
}
