package dev.axiom.lyrics;

import java.util.List;

public final class LyricsState {
    public record LrcLine(long ms, String text) {}

    public static volatile boolean enabled = true;
    public static volatile String trackId = "";
    public static volatile String status = "Nicht eingeloggt: /lyrics login <clientId>";
    public static volatile boolean playing;
    public static volatile long progressMs, fetchedAt;
    public static volatile int offsetMs = 0;
    public static volatile List<LrcLine> lines = List.of();

    /** Interpolierte Songposition in ms. */
    public static long now() {
        return playing ? progressMs + (System.currentTimeMillis() - fetchedAt) : progressMs;
    }
    private LyricsState() {}
}
