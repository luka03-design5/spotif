package dev.axiom.lyrics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class LyricsClient implements ClientModInitializer {
    private static String lastStatus = "";

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
                .then(literal("size").then(argument("prozent", IntegerArgumentType.integer(40, 400))
                    .executes(c -> {
                        int pct = IntegerArgumentType.getInteger(c, "prozent");
                        LyricsState.size = pct / 100f;
                        c.getSource().sendFeedback(Text.literal("Größe: " + pct + " %"));
                        return 1;
                    })))
                .then(literal("hud").executes(c -> {
                    LyricsState.hud = !LyricsState.hud;
                    c.getSource().sendFeedback(Text.literal("Zeile zusätzlich in der Actionbar: "
                        + (LyricsState.hud ? "an" : "aus")));
                    return 1;
                }))
                .then(literal("debug").executes(c -> {
                    c.getSource().sendFeedback(Text.literal("Status: " + LyricsState.status));
                    c.getSource().sendFeedback(Text.literal("Song: " + LyricsState.title + " – " + LyricsState.artist));
                    c.getSource().sendFeedback(Text.literal("Zeilen: " + LyricsState.lines.size()
                        + " | spielt: " + LyricsState.playing
                        + " | Position: " + LyricsState.now() + " ms"
                        + " | an: " + LyricsState.enabled));
                    return 1;
                }))
            ));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(LyricsRenderer::render);

        // Statusmeldungen (z. B. "Keine Lyrics gefunden") in der Actionbar zeigen,
        // dazu optional die aktuelle Zeile (/lyrics hud).
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !LyricsState.enabled) return;
            if (LyricsState.lines.isEmpty()) {
                String s = LyricsState.status;
                if (!s.equals(lastStatus)) {
                    lastStatus = s;
                    client.inGameHud.setOverlayMessage(Text.literal(s), false);
                }
            } else {
                lastStatus = "";
                if (LyricsState.hud) {
                    String line = LyricsState.currentText();
                    if (!line.isBlank()) client.inGameHud.setOverlayMessage(Text.literal(line), false);
                }
            }
        });
    }
}
