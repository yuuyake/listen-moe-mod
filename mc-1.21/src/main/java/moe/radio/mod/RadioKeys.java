package moe.radio.mod;

import com.mojang.blaze3d.platform.InputConstants;
import moe.radio.core.RadioPlayer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class RadioKeys {
    private static KeyMapping pause, play, next;

    private RadioKeys() {}

    static void init() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("listenmoe", "main"));
        pause = reg("pause", GLFW.GLFW_KEY_F6, cat);
        play = reg("play", GLFW.GLFW_KEY_F7, cat);
        next = reg("switch", GLFW.GLFW_KEY_F8, cat);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (pause.consumeClick()) RadioPlayer.pause();
            while (play.consumeClick()) RadioPlayer.resume();
            while (next.consumeClick()) RadioPlayer.switchStation();
        });
    }

    private static KeyMapping reg(String name, int key, KeyMapping.Category cat) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.listenmoe." + name, InputConstants.Type.KEYSYM, key, cat));
    }

    private static String label(KeyMapping k) {
        return k == null || k.isUnbound() ? "-" : k.getTranslatedKeyMessage().getString();
    }

    static String pause() { return label(pause); }
    static String play() { return label(play); }
    static String next() { return label(next); }
}
