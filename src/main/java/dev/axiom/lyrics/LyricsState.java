package dev.axiom.lyrics;

import java.util.List;

public final class LyricsState {
    public record LrcLine(long ms, String text) {}

    public static volatile boolean enabled = true;
    public static volatile boolean hud = false;          // Zeile zusätzlich in der Actionbar zeigen
    public static volatile float size = 1.0f;            // Größenfaktor der 3D-Schrift
    public static volatile String trackId = "";
    public static volatile String title = "", artist = "";
    public static volatile String status = "Nicht eingeloggt: /lyrics login <clientId>";
    public static volatile boolean playing;
    public static volatile long progressMs, fetchedAt;
    public static volatile int offsetMs = 0;
    public static volatile List<LrcLine> lines = List.of();

    /** Interpolierte Songposition in ms. */
    public static long now() {
        return playing ? progressMs + (System.currentTimeMillis() - fetchedAt) : progressMs;
    }

    /** Index der aktuellen Zeile zum Zeitpunkt t (-1 = noch vor der ersten Zeile). */
    public static int indexAt(List<LrcLine> l, long t) {
        int i = -1;
        for (int k = 0; k < l.size(); k++) {
            if (l.get(k).ms() <= t) i = k; else break;
        }
        return i;
    }

    public static String currentText() {
        List<LrcLine> l = lines;
        int i = indexAt(l, now() + offsetMs);
        return i >= 0 ? l.get(i).text() : "";
    }

    private LyricsState() {}
}
