package dev.axiom.lyrics;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Zeichnet jede Lyrics-Zeile als 3D-Schrift an einer festen Stelle in der Luft vor dem Spieler.
 * Die Wörter erscheinen nacheinander, mit leichtem Versatz, Neigung und Leuchten.
 */
public final class LyricsRenderer {
    private static final int MAX_CHARS = 16;      // Zeichen pro Zeile
    private static final float ROW_H = 17f;       // Zeilenabstand in Schrift-Einheiten
    private static final float BASE_SCALE = 0.09f;

    private static final int WHITE = 0xF3ECFF;    // aktuelle Zeile
    private static final int LILAC = 0xB79CFF;    // Zeile ist vorbei
    private static final int GLOW  = 0xA06CFF;
    // Tiefe der 3D-Schrift, von vorne (hell) nach hinten (dunkel)
    private static final int[] EXTRUDE = {0x6A3FE0, 0x5A34C4, 0x4A2AA8, 0x3A218C, 0x2B1770};

    private record Row(List<String> words, float[] widths, float width, int firstWord, float xOff, float roll) {}
    private record Phrase(Vec3d anchor, Quaternionf rot, List<Row> rows, long start, long dur, float step) {}

    private static final Map<Integer, Phrase> PHRASES = new HashMap<>();
    private static String cacheTrack = "";

    public static void render(WorldRenderContext ctx) {
        if (!LyricsState.enabled) return;

        String track = LyricsState.trackId;
        if (!track.equals(cacheTrack)) { PHRASES.clear(); cacheTrack = track; }

        List<LyricsState.LrcLine> lines = LyricsState.lines;
        if (lines.isEmpty()) return;

        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider vc = ctx.consumers();
        if (m == null || vc == null) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Camera cam = ctx.camera();

        long t = LyricsState.now() + LyricsState.offsetMs;
        final int cur = LyricsState.indexAt(lines, t);
        if (cur < 0) return;

        PHRASES.keySet().removeIf(k -> k < cur - 2 || k > cur);

        Vec3d camPos = cam.getPos();
        for (int k = Math.max(0, cur - 2); k <= cur; k++) {
            Phrase p = PHRASES.get(k);
            if (p == null) {
                if (k != cur) continue;               // nur die aktuelle Zeile neu erzeugen
                p = spawn(k, lines, cam, tr);
                if (p == null) continue;
                PHRASES.put(k, p);
            }
            drawPhrase(p, t, camPos, tr, m, vc);
        }

        if (vc instanceof VertexConsumerProvider.Immediate imm) imm.draw();
    }

    private static Phrase spawn(int k, List<LyricsState.LrcLine> lines, Camera cam, TextRenderer tr) {
        LyricsState.LrcLine ln = lines.get(k);
        String text = ln.text().trim();
        if (text.isEmpty()) return null;

        long end = k + 1 < lines.size() ? lines.get(k + 1).ms() : ln.ms() + 4000;
        long dur = Math.max(1200, Math.min(6000, end - ln.ms()));
        Random rnd = new Random(31L * k + LyricsState.trackId.hashCode());

        // Wörter in Zeilen packen
        String[] ws = text.split("\\s+");
        List<Row> rows = new ArrayList<>();
        List<String> bucket = new ArrayList<>();
        int chars = 0, gi = 0, first = 0;
        for (String w : ws) {
            if (!bucket.isEmpty() && chars + 1 + w.length() > MAX_CHARS) {
                rows.add(makeRow(tr, bucket, first, rows.size(), rnd));
                first = gi;
                bucket = new ArrayList<>();
                chars = 0;
            }
            bucket.add(w);
            chars += (chars == 0 ? 0 : 1) + w.length();
            gi++;
        }
        if (!bucket.isEmpty()) rows.add(makeRow(tr, bucket, first, rows.size(), rnd));

        float step = Math.min(420f, dur * 0.7f / Math.max(1, ws.length));

        // Position: vor dem Spieler, aber seitlich versetzt und etwas höher
        float yaw = cam.getYaw();
        float pitch = Math.max(-20f, Math.min(35f, cam.getPitch()));
        double dist = 8.0 + rnd.nextDouble() * 3.0;
        Vec3d fwd = Vec3d.fromPolar(pitch, yaw);
        Vec3d right = Vec3d.fromPolar(0f, yaw + 90f);
        double side = (k % 2 == 0 ? -1 : 1) * (0.10 + rnd.nextDouble() * 0.35);
        double up = -0.03 + rnd.nextDouble() * 0.28;
        Vec3d pos = cam.getPos()
            .add(fwd.multiply(dist))
            .add(right.multiply(side * dist))
            .add(0, up * dist, 0);

        // Ausrichtung wie die Kamera in diesem Moment, plus leichte Schräglage
        Quaternionf rot = new Quaternionf(cam.getRotation())
            .rotateZ((float) Math.toRadians((rnd.nextFloat() - 0.5f) * 8f));

        return new Phrase(pos, rot, rows, ln.ms(), dur, step);
    }

    private static Row makeRow(TextRenderer tr, List<String> words, int first, int r, Random rnd) {
        float[] wd = new float[words.size()];
        float total = 0;
        for (int i = 0; i < wd.length; i++) {
            wd[i] = tr.getWidth(words.get(i));
            total += wd[i] + (i > 0 ? 4f : 0f);
        }
        float xOff = (r % 2 == 0 ? -1 : 1) * (3f + rnd.nextFloat() * 20f);
        float roll = (rnd.nextFloat() - 0.5f) * 10f;
        return new Row(words, wd, total, first, xOff, roll);
    }

    private static void drawPhrase(Phrase p, long t, Vec3d camPos, TextRenderer tr,
                                   MatrixStack m, VertexConsumerProvider vc) {
        if (t < p.start()) return;
        long end = p.start() + p.dur();

        float fade = 1f;
        long fadeStart = end + 900;
        if (t > fadeStart) fade = 1f - (t - fadeStart) / 1400f;
        if (fade <= 0.02f) return;

        Vec3d rel = p.anchor().subtract(camPos);
        if (rel.lengthSquared() > 70.0 * 70.0) return;
        int main = t > end ? LILAC : WHITE;

        m.push();
        m.translate(rel.x, rel.y, rel.z);
        m.multiply(p.rot());
        float s = BASE_SCALE * LyricsState.size;
        m.scale(s, -s, s);

        float totalH = p.rows().size() * ROW_H;
        for (int r = 0; r < p.rows().size(); r++) {
            Row row = p.rows().get(r);
            m.push();
            m.translate(row.xOff(), r * ROW_H - totalH / 2f, 0);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(row.roll()));

            float x = -row.width() / 2f;
            for (int w = 0; w < row.words().size(); w++) {
                float ww = row.widths()[w];
                int gi = row.firstWord() + w;
                float prog = (t - (p.start() + gi * p.step())) / 260f;
                if (prog > 0f) {
                    float e = 1f - (float) Math.pow(1f - Math.min(1f, prog), 3);
                    int alpha = (int) (255 * e * fade);
                    float bob = (float) Math.sin((t + gi * 230L) / 650.0) * 0.8f;
                    float pop = 0.7f + 0.3f * e;
                    m.push();
                    m.translate(x + ww / 2f, 4f + bob + (1f - e) * 6f, 0);
                    m.scale(pop, pop, 1f);
                    word(tr, m, vc, row.words().get(w), -ww / 2f, -4f, alpha, main);
                    m.pop();
                }
                x += ww + 4f;
            }
            m.pop();
        }
        m.pop();
    }

    private static void word(TextRenderer tr, MatrixStack m, VertexConsumerProvider vc,
                             String w, float x, float y, int alpha, int main) {
        if (alpha < 8) return;

        int ga = alpha / 5;                                   // Leuchten
        if (ga >= 8) {
            for (int dx = -1; dx <= 1; dx += 2)
                for (int dy = -1; dy <= 1; dy += 2)
                    put(tr, m, vc, w, x + dx * 1.3f, y + dy * 1.3f, -4.5f, argb(ga, GLOW));
        }
        for (int i = EXTRUDE.length - 1; i >= 0; i--) {      // Tiefe, hinten zuerst
            put(tr, m, vc, w, x, y, -(i + 1) * 0.9f, argb((int) (alpha * 0.95f), EXTRUDE[i]));
        }
        put(tr, m, vc, w, x, y, 0f, argb(alpha, main));       // Vorderseite
    }

    private static void put(TextRenderer tr, MatrixStack m, VertexConsumerProvider vc,
                            String w, float x, float y, float z, int color) {
        m.push();
        m.translate(0, 0, z);
        tr.draw(w, x, y, color, false, m.peek().getPositionMatrix(), vc,
            TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        m.pop();
    }

    private static int argb(int a, int rgb) {
        return (Math.min(255, Math.max(0, a)) << 24) | rgb;
    }

    private LyricsRenderer() {}
}
