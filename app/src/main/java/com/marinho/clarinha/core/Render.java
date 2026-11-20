package com.marinho.clarinha.core;

/** Desenha o mundo (personagens, acessórios, partículas, balões). */
public final class Render {
    private Render() {}

    static float sin(float a) { return (float) Math.sin(a); }
    static float cos(float a) { return (float) Math.cos(a); }

    public static void drawTwinkles(G g, World w, float[][] tw) {
        for (float[] s : tw) {
            g.save(); g.mulAlpha(.5f + .5f * sin(w.T * 2 + s[2]));
            Painter.starPath(g, s[0], s[1], s[3] * 2.2f, s[3] * .7f, 0); g.fill(0xFFFFFFFF);
            g.restore();
        }
    }

    public static void drawStars(G g, World w) {
        float S = w.S;
        for (float[] s : w.stars) {
            float rot = sin(w.T * 2 + s[0]) * .3f;
            g.save();
            if (s[4] < 3) g.mulAlpha(sin(w.T * 12) > 0 ? 1 : .4f);
            g.glow(0xFFFFE066, 14 * S / 1.3f);
            Painter.starPath(g, s[0], s[1], 7 * S, 3 * S, rot); g.fill(0xFFFFE066);
            g.noGlow();
            g.stroke(Painter.OUT, 1.2f * S / 1.3f);
            g.restore();
        }
    }

    /** Escada, corda, sombra, personagem e estrelinhas de tontura. */
    public static void drawPet(G g, World w, Pet p, boolean shadow, boolean selected) {
        float S = w.S;
        if (p.ladderA > 0) {
            float top = p.ladderBot - (p.ladderBot - p.ladderTop) * ("ladderPlace".equals(p.state) ? ease(p.ladderProg) : 1);
            Painter.drawLadder(g, p.ladderX, top, p.ladderBot, S, p.ladderA);
        }
        if (p.ropeA > 0) {
            float bot = "ropeTie".equals(p.state) ? p.ropeTop + (w.floorY - p.ropeTop) * ease(p.ropeProg) : w.floorY;
            float sway = "rope".equals(p.state) ? sin(p.ph) * 6 * S : 0;
            Painter.drawRope(g, p.ropeX, p.ropeTop, bot, S, sway, p.ropeA);
        }
        boolean surf = w.onSurface(p);
        if (shadow && !"hang".equals(p.state) && !"climb".equals(p.state) && !"rope".equals(p.state) && !"ladderUp".equals(p.state)) {
            float[] c = w.l2w(p, 0, -45);
            float gy = p.plat >= 0 && p.plat < w.plats.size() ? w.plats.get(p.plat)[1] : w.floorY;
            float hgt = surf ? 0 : World.clamp((w.floorY - Math.max(p.y, c[1] + 45 * S)) / (5 * w.CH), 0, .8f);
            if (surf || !w.overlay) {
                float rw = ("sleep".equals(p.state) ? 40 : 18) * S * (1 - hgt * .6f);
                g.begin(); g.ellipse(c[0], (surf ? gy : w.floorY) + 1, rw, 4.2f * S * (1 - hgt * .6f), 0);
                g.fill(((int) (255 * .22f * (1 - hgt)) << 24));
            }
        }
        if (selected) {
            float[] c = w.l2w(p, 0, -50);
            g.dash(true, 6 * w.dp);
            g.begin(); g.arc(c[0], c[1], 62 * S, 0, (float) Math.PI * 2); g.stroke(0xE6FFFFFF, 2.5f * w.dp);
            g.dash(false, 0);
        }
        float[] X = w.xform(p);
        g.save(); g.translate(X[0], X[1]); g.rotate(X[2]); g.translate(X[3], X[4]); g.scale(X[5], X[6]);
        Painter.drawChar(g, Skin.ALL[p.skin], w.pose(p), w.T);
        g.restore();
        if ("dizzy".equals(p.state) || "bump".equals(p.state)) {
            float[] h = w.l2w(p, 0, -90);
            for (int i = 0; i < 3; i++) {
                float a = w.T * 4 + i * (float) Math.PI * 2 / 3;
                Painter.starPath(g, h[0] + cos(a) * 24 * S, h[1] + sin(a) * 6 * S, 5 * S, 2.2f * S, a);
                g.fill(0xFFFFE066); g.stroke(Painter.OUT, 1 * S);
            }
        }
    }

    static float ease(float k) { return 1 - (1 - k) * (1 - k); }

    /** owner = 0 desenha todas; senão só as daquela personagem. */
    public static void drawParts(G g, World w, int owner) {
        float S = w.S;
        for (World.Part q : w.parts) {
            if (owner != 0 && q.owner != owner) continue;
            float a = World.clamp(q.life / q.max, 0, 1), z = S * q.s;
            g.save(); g.mulAlpha(a);
            switch (q.type) {
                case World.P_SPARK: Painter.starPath(g, q.x, q.y, 4 * z, 1.6f * z, q.r); g.fill(q.col); break;
                case World.P_STAR: Painter.starPath(g, q.x, q.y, 6 * z, 2.6f * z, q.r); g.fill(0xFFFFE066); g.stroke(Painter.OUT, z * .8f); break;
                case World.P_HEART:
                    Painter.heartPath(g, q.x + sin(q.life * 6) * 4 * z, q.y, 7 * z); g.fill(0xFFFF6FA8); g.stroke(0xFFFFFFFF, 1.2f * z * .8f); break;
                case World.P_Z: g.text("z", q.x, q.y, 16 * z * (1.3f - a * .4f), 0xFFFFFFFF, 0xFF6B55A0, 3 * z * .7f); break;
                case World.P_NOTE:
                    g.begin(); g.ellipse(q.x, q.y, 4 * z, 3 * z, -.4f); g.fill(q.col); g.stroke(Painter.OUT, z * .8f);
                    g.begin(); g.rect(q.x + 3 * z, q.y - 12 * z, 1.8f * z, 12 * z); g.fill(q.col);
                    break;
                case World.P_RB: g.begin(); g.ellipse(q.x, q.y, q.s, q.s, 0); g.fill(q.col); break;
                default: break;
            }
            g.restore();
        }
    }

    public static float bubbleFont(World w) { return World.clamp(w.CH * .15f, 13 * w.dp, 22 * w.dp); }

    /** x, y (canto sup. esq.), largura, altura, ponta x, embaixo(1/0) */
    public static float[] bubbleRect(G g, World w, Pet p) {
        float fsz = bubbleFont(w);
        float[] c = w.l2w(p, 0, -50);
        float y = c[1] - ("sleep".equals(p.state) ? 40 * w.S : 62 * w.S);
        float tw = g != null ? g.textWidth(p.bub, fsz) : p.bub.length() * fsz * .62f;
        float bw = tw + fsz * 1.2f, bh = fsz * 1.75f;
        boolean under = y - bh < w.topY + 6;
        if (under) y = c[1] + 60 * w.S + bh;
        float x = World.clamp(c[0], bw / 2 + 6, w.W - bw / 2 - 6);
        float bx = x - bw / 2, by = y - bh;
        float tx = World.clamp(c[0], bx + bh / 2, bx + bw - bh / 2);
        return new float[]{bx, by, bw, bh, tx, under ? 1 : 0};
    }

    public static void drawBubble(G g, World w, Pet p) {
        if (p.bub == null) return;
        float[] r = bubbleRect(g, w, p);
        float bx = r[0], by = r[1], bw = r[2], bh = r[3], tx = r[4];
        boolean under = r[5] > 0;
        float fsz = bubbleFont(w), lw = 2 * w.dp;
        g.save(); g.mulAlpha(World.clamp(p.bubT * 4, 0, 1));
        g.begin(); g.roundRect(bx, by, bw, bh, bh / 2); g.fill(0xFFFFFFFF); g.stroke(0xFF6B4AA8, lw);
        g.begin();
        float k = 6 * w.dp, tl = 8 * w.dp;
        if (!under) { g.moveTo(tx - k, by + bh - 1); g.lineTo(tx, by + bh + tl); g.lineTo(tx + k, by + bh - 1); }
        else { g.moveTo(tx - k, by + 1); g.lineTo(tx, by - tl); g.lineTo(tx + k, by + 1); }
        g.fill(0xFFFFFFFF); g.stroke(0xFF6B4AA8, lw);
        g.begin(); g.rect(tx - k + lw, under ? by + lw * .5f : by + bh - lw * 1.6f, 2 * k - 2 * lw, lw * 1.6f); g.fill(0xFFFFFFFF);
        g.text(p.bub, bx + bw / 2, by + bh / 2, fsz, 0xFF3A2558, 0, 0);
        g.restore();
    }

    /** Caixa que envolve tudo o que a personagem desenha (para a janela flutuante). */
    public static void bounds(World w, Pet p, float[] out) {
        float S = w.S;
        float l = 1e9f, t = 1e9f, r = -1e9f, b = -1e9f;
        float[][] pts = {{-44, -124}, {44, -124}, {44, 10}, {-44, 10}};
        for (float[] q : pts) { float[] c = w.l2w(p, q[0], q[1]); l = Math.min(l, c[0]); r = Math.max(r, c[0]); t = Math.min(t, c[1]); b = Math.max(b, c[1]); }
        if ("dizzy".equals(p.state) || "bump".equals(p.state)) { float[] h = w.l2w(p, 0, -90); l = Math.min(l, h[0] - 32 * S); r = Math.max(r, h[0] + 32 * S); }
        if (p.bub != null) {
            float[] br = bubbleRect(null, w, p);
            l = Math.min(l, br[0] - 4); t = Math.min(t, br[1] - 12 * w.dp); r = Math.max(r, br[0] + br[2] + 4); b = Math.max(b, br[1] + br[3] + 12 * w.dp);
        }
        if (p.ladderA > 0) { l = Math.min(l, p.ladderX - 12 * S); r = Math.max(r, p.ladderX + 12 * S); t = Math.min(t, p.ladderTop - 4 * S); b = Math.max(b, p.ladderBot + 4 * S); }
        if (p.ropeA > 0) { l = Math.min(l, p.ropeX - 14 * S); r = Math.max(r, p.ropeX + 14 * S); t = Math.min(t, p.ropeTop - 6 * S); b = Math.max(b, w.floorY + 4 * S); }
        for (World.Part q : w.parts) {
            if (q.owner != p.id) continue;
            l = Math.min(l, q.x - 14 * S); r = Math.max(r, q.x + 14 * S); t = Math.min(t, q.y - 20 * S); b = Math.max(b, q.y + 14 * S);
        }
        if (w.onSurface(p)) b = Math.max(b, p.y + 8 * S);
        out[0] = l - 4 * S; out[1] = t - 4 * S; out[2] = r + 4 * S; out[3] = b + 4 * S;
    }
}
