package moe.radio.mod;

import com.mojang.blaze3d.platform.NativeImage;
import moe.radio.core.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

final class RadioCover {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("listenmoe", "cover");
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
                for (int xx = 0; xx < S; xx++) img.setPixel(xx, yy, px[yy * S + xx]);
            }
            Minecraft.getInstance().getTextureManager().register(ID, new DynamicTexture(() -> "listenmoe_cover", img));
            has = true;
        } catch (Exception e) {
            has = false;
        }
    }

    static boolean draw(GuiGraphicsExtractor g, int x, int y, int size) {
        sync();
        if (!has) return false;
        g.blit(RenderPipelines.GUI_TEXTURED, ID, x, y, 0f, 0f, size, size, S, S, S, S);
        return true;
    }
}
