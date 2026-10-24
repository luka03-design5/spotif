package dev.axiom.lyrics;

import com.google.gson.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public final class LrcLib {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final Pattern LINE = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)](.*)$");

    public static List<LyricsState.LrcLine> fetch(String title, String artist, long durSec) {
        List<LyricsState.LrcLine> r = tryFetch(title, artist, durSec);
        if (r.isEmpty()) {
            String cleaned = title.replaceAll("\\s+-\\s+.*$", "").replaceAll("\\s*\\(.*?\\)", "").trim();
            if (!cleaned.equals(title)) r = tryFetch(cleaned, artist, durSec);
        }
        return r;
    }

    private static List<LyricsState.LrcLine> tryFetch(String title, String artist, long durSec) {
        try {
            String url = "https://lrclib.net/api/get?track_name=" + enc(title)
                + "&artist_name=" + enc(artist) + "&duration=" + durSec;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "SpotifyLyricsMod/1.0 (Fabric client mod)").GET().build();
            HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return List.of();
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return List.of();
            return parse(o.get("syncedLyrics").getAsString());
        } catch (Exception e) {
            return List.of();
        }
    }

    static List<LyricsState.LrcLine> parse(String lrc) {
        List<LyricsState.LrcLine> out = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher m = LINE.matcher(raw.trim());
            if (!m.matches()) continue;
            long ms = (long) ((Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000);
            out.add(new LyricsState.LrcLine(ms, m.group(3).trim()));
        }
        out.sort(Comparator.comparingLong(LyricsState.LrcLine::ms));
        return out;
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private LrcLib() {}
}
