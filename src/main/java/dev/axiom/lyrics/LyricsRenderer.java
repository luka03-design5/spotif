package dev.axiom.lyrics;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class LyricsRenderer {
    private static final double DIST = 3.0;
    private static final float MAX_W = 260f;
    private static final int SPOTIFY_GREEN = 0xFF1DB954;

    public static void render(WorldRenderContext ctx) {
        if (!LyricsState.enabled) return;
        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider vc = ctx.consumers();
        if (m == null || vc == null) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Camera cam = ctx.camera();

        String prev = "", cur = "", next = "";
        List<LyricsState.LrcLine> lines = LyricsState.lines;
        if (lines.isEmpty()) {
            cur = LyricsState.status;
        } else {
            long t = LyricsState.now() + LyricsState.offsetMs;
            int i = -1;
            for (int k = 0; k < lines.size(); k++) {
                if (lines.get(k).ms() <= t) i = k; else break;
            }
            if (i > 0) prev = lines.get(i - 1).text();
            if (i >= 0) cur = lines.get(i).text();
            if (i + 1 < lines.size()) next = lines.get(i + 1).text();
        }

        Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
        m.push();
        m.translate(look.x * DIST, look.y * DIST, look.z * DIST);
        m.multiply(cam.getRotation());
        m.scale(0.025f, -0.025f, 0.025f);

        drawLine(tr, m, vc, prev, -16f, 0x99FFFFFF);
        drawLine(tr, m, vc, cur,    0f, SPOTIFY_GREEN);
        drawLine(tr, m, vc, next,  16f, 0x99FFFFFF);

        m.pop();
        if (vc instanceof VertexConsumerProvider.Immediate imm) imm.draw();
    }

    private static void drawLine(TextRenderer tr, MatrixStack m, VertexConsumerProvider vc,
                                 String s, float y, int color) {
        if (s == null || s.isBlank()) return;
        float w = tr.getWidth(s);
        float sc = Math.min(1f, MAX_W / w);
        m.push();
        m.translate(0, y, 0);
        m.scale(sc, sc, 1f);
        tr.draw(s, -w / 2f, 0, color, false, m.peek().getPositionMatrix(), vc,
            TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, 0xF000F0);
        m.pop();
    }
    private LyricsRenderer() {}
}
