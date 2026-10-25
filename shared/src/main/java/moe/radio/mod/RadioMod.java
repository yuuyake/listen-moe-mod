package moe.radio.mod;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import moe.radio.core.HudState;
import moe.radio.core.NowPlaying;
import moe.radio.core.RadioPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class RadioMod implements ClientModInitializer {
    private static Path cfg;

    static Path cfg() { return cfg; }

    @Override
    public void onInitializeClient() {
        cfg = FabricLoader.getInstance().getConfigDir().resolve("listenmoe.properties");
        HudState.load(cfg);
        RadioHud.register();
        RadioKeys.init();

        LiteralArgumentBuilder<FabricClientCommandSource> hud = literal("hud").executes(c -> {
            HudState.enabled = !HudState.enabled;
            HudState.save(cfg);
            return msg(c, "HUD " + (HudState.enabled ? "on" : "off"));
        });
        hud.then(flag("buttons", () -> HudState.showButtons, v -> HudState.showButtons = v));
        hud.then(flag("cover", () -> HudState.showCover, v -> HudState.showCover = v));
        hud.then(flag("progress", () -> HudState.showProgress, v -> HudState.showProgress = v));
        hud.then(flag("lyrics", () -> HudState.showLyrics, v -> HudState.showLyrics = v));
        hud.then(literal("scale").then(argument("percent", IntegerArgumentType.integer(50, 200)).executes(c -> {
            int p = IntegerArgumentType.getInteger(c, "percent");
            Minecraft mc = Minecraft.getInstance();
            HudState.setScale(p / 100.0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            HudState.save(cfg);
            return msg(c, "HUD scale: " + p + "%");
        })));
        hud.then(literal("reset").executes(c -> {
            HudState.reset();
            HudState.save(cfg);
            return msg(c, "HUD reset");
        }));
        for (HudState.Corner k : HudState.Corner.values()) {
            hud.then(literal(k.arg).executes(c -> {
                HudState.setCorner(k);
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
                .then(literal("np").executes(c -> msg(c, "Now playing: " + NowPlaying.describe())))
                .then(literal("raw").executes(c -> msg(c, NowPlaying.lastRaw())))
                .then(literal("lyrics").then(literal("offset")
                    .executes(c -> msg(c, "Lyrics offset: " + HudState.lyricsOffsetMs + " ms"))
                    .then(argument("ms", IntegerArgumentType.integer(-15000, 15000)).executes(c -> {
                        HudState.lyricsOffsetMs = IntegerArgumentType.getInteger(c, "ms");
                        HudState.save(cfg);
                        return msg(c, "Lyrics offset: " + HudState.lyricsOffsetMs + " ms");
                    }))))
                .then(literal("volume")
                    .executes(c -> msg(c, "Volume: " + RadioPlayer.getVolume()))
                    .then(argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> {
                        RadioPlayer.setVolume(IntegerArgumentType.getInteger(c, "percent"));
                        HudState.save(cfg);
                        return msg(c, "Volume: " + RadioPlayer.getVolume());
                    })))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> flag(String name, BooleanSupplier get, Consumer<Boolean> set) {
        return literal(name)
            .executes(c -> {
                set.accept(!get.getAsBoolean());
                HudState.save(cfg);
                return msg(c, "HUD " + name + " " + (get.getAsBoolean() ? "on" : "off"));
            })
            .then(argument("show", BoolArgumentType.bool()).executes(c -> {
                set.accept(BoolArgumentType.getBool(c, "show"));
                HudState.save(cfg);
                return msg(c, "HUD " + name + " " + (get.getAsBoolean() ? "on" : "off"));
            }));
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
