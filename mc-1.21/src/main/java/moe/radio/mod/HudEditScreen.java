package moe.radio.mod;

import moe.radio.core.HudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class HudEditScreen extends Screen {
    private boolean dragging, wasDown;
    private int offX, offY;

    HudEditScreen() { super(Component.literal("Edit HUD")); }

    @Override
    protected void init() { HudState.editing = true; }

    @Override
    public void removed() {
        HudState.editing = false;
        HudState.save(RadioMod.cfg());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        int sw = this.width, sh = this.height;
        int x = HudState.x(sw), y = HudState.y(sh);
        int w = HudState.WIDTH, h = HudState.HEIGHT;

        boolean down = mc.mouseHandler.isLeftPressed();
        if (down && !wasDown && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            dragging = true;
            offX = mouseX - x;
            offY = mouseY - y;
        }
        if (!down) dragging = false;
        wasDown = down;
        if (dragging) {
            HudState.setPos(mouseX - offX, mouseY - offY, sw, sh);
            x = HudState.x(sw);
            y = HudState.y(sh);
        }

        g.fill(0, 0, sw, sh, 0x50000000);
        RadioHud.draw(g, x, y);
        int c = dragging ? 0xFFFFFFFF : 0xFF8888FF;
        g.fill(x - 1, y - 1, x + w + 1, y, c);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, c);
        g.fill(x - 1, y, x, y + h, c);
        g.fill(x + w, y, x + w + 1, y + h, c);
        String hint = "Drag the player to move it. Esc to save.";
        g.drawString(mc.font, hint, (sw - mc.font.width(hint)) / 2, 12, 0xFFFFFFFF);
    }
}
