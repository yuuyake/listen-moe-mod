package moe.radio.mod;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import moe.radio.core.HudState;
import moe.radio.core.NowPlaying;
import moe.radio.core.RadioPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/** Mojang mappings */
public class RadioMod implements ClientModInitializer {
    private static Path cfg;

    @Override
    public void onInitializeClient() {
        cfg = FabricLoader.getInstance().getConfigDir().resolve("listenmoe.properties");
        HudState.load(cfg);
        RadioHud.register();

        LiteralArgumentBuilder<FabricClientCommandSource> hud = literal("hud").executes(c -> {
            HudState.enabled = !HudState.enabled;
            HudState.save(cfg);
            return msg(c, "HUD " + (HudState.enabled ? "on" : "off"));
        });
        for (HudState.Corner k : HudState.Corner.values()) {
            hud.then(literal(k.arg).executes(c -> {
                HudState.corner = k;
                HudState.enabled = true;
                HudState.save(cfg);
                return msg(c, "HUD position: " + k.arg);
            }));
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("radio")
                .then(literal("play")
                    .executes(c -> play(c, RadioPlayer.JPOP, "J-POP"))
                    .then(literal("kpop").executes(c -> play(c, RadioPlayer.KPOP, "K-POP"))))
                .then(literal("stop").executes(c -> { RadioPlayer.stop(); return msg(c, "Radio stopped"); }))
                .then(hud)
                .then(literal("np").executes(c -> msg(c, "Now playing: " + NowPlaying.get())))
                .then(literal("volume")
                    .executes(c -> msg(c, "Volume: " + RadioPlayer.getVolume()))
                    .then(argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> {
                        RadioPlayer.setVolume(IntegerArgumentType.getInteger(c, "percent"));
                        HudState.save(cfg);
                        return msg(c, "Volume: " + RadioPlayer.getVolume());
                    })))));
    }

    private static int play(CommandContext<FabricClientCommandSource> c, String url, String name) {
        RadioPlayer.play(url);
        return msg(c, "Radio playing (" + name + ")");
    }

    private static int msg(CommandContext<FabricClientCommandSource> c, String s) {
        c.getSource().sendFeedback(Component.literal(s));
        return 1;
    }
}
