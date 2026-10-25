package moe.radio.mod;

import com.mojang.blaze3d.platform.NativeImage;
import moe.radio.core.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class RadioCover {
    private static final ResourceLocation ID = new ResourceLocation("listenmoe", "cover");
    private static final int S = NowPlaying.COVER_SIZE;
    private static int uploaded = -1;
    private static boolean has;

    private RadioCover() {}

    private static void sync() {
        int v = NowPlaying.coverVersion();
        if (v == uploaded) return;
        uploaded = v;
        has = false;
        int[] px = NowPlaying.coverPixels();
        if (px == null || px.length != S * S) return;
        try {
            NativeImage img = new NativeImage(S, S, false);
            for (int yy = 0; yy < S; yy++) {
                for (int xx = 0; xx < S; xx++) {
                    int p = px[yy * S + xx];
                    img.setPixelRGBA(xx, yy, (p & 0xFF00FF00) | ((p & 0xFF) << 16) | ((p >> 16) & 0xFF));
                }
            }
            Minecraft.getInstance().getTextureManager().register(ID, new DynamicTexture(img));
            has = true;
        } catch (Exception e) {
            has = false;
        }
    }

    static boolean draw(GuiGraphics g, int x, int y, int size) {
        sync();
        if (!has) return false;
        g.blit(ID, x, y, size, size, 0f, 0f, S, S, S, S);
        return true;
    }
}
