package dev.axiom.lyrics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class LyricsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpotifyAuth.load();
        SpotifyPoller.start();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(literal("lyrics")
                .then(literal("login")
                    .executes(c -> {
                        SpotifyAuth.login(null);
                        c.getSource().sendFeedback(Text.literal("Browser öffnet sich …"));
                        return 1;
                    })
                    .then(argument("clientId", StringArgumentType.word()).executes(c -> {
                        SpotifyAuth.login(StringArgumentType.getString(c, "clientId"));
                        c.getSource().sendFeedback(Text.literal("Browser öffnet sich …"));
                        return 1;
                    })))
                .then(literal("toggle").executes(c -> {
                    LyricsState.enabled = !LyricsState.enabled;
                    c.getSource().sendFeedback(Text.literal("Lyrics: " + (LyricsState.enabled ? "an" : "aus")));
                    return 1;
                }))
                .then(literal("offset").then(argument("ms", IntegerArgumentType.integer(-5000, 5000))
                    .executes(c -> {
                        LyricsState.offsetMs = IntegerArgumentType.getInteger(c, "ms");
                        c.getSource().sendFeedback(Text.literal("Offset: " + LyricsState.offsetMs + " ms"));
                        return 1;
                    })))
            ));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(LyricsRenderer::render);
    }
}
