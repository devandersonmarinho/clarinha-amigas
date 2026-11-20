package com.marinho.clarinha.core;

import java.util.Random;

/** Desenho da personagem, dos acessórios e dos cenários. */
public final class Painter {
    public static final int OUT = 0xFF2D1B42, SKIN = 0xFFFFE2CF, HAIR = 0xFF6B3E24, HAIR_HI = 0xFF93603A,
            GLASS = 0xFF8B4DFF, EYE = 0xFF3A2216, WHITE = 0xFFFFFFFF, MOUTH = 0xFF8A2F42, TONGUE = 0xFFFF8FA6;
    static final float PI = (float) Math.PI, TAU = PI * 2;

    private Painter() {}

    static float sin(float a) { return (float) Math.sin(a); }
    static float cos(float a) { return (float) Math.cos(a); }

    static void ell(G g, float x, float y, float rx, float ry, float rot) { g.begin(); g.ellipse(x, y, rx, ry, rot); }
    static void fs(G g, int fill) { fs(g, fill, 1.5f); }
    static void fs(G g, int fill, float lw) { g.fill(fill); g.stroke(OUT, lw); }
    static void limb(G g, float x1, float y1, float x2, float y2, float w, int col) {
        g.begin(); g.moveTo(x1, y1); g.lineTo(x2, y2); g.stroke(OUT, w + 2.8f); g.stroke(col, w);
    }
    public static void starPath(G g, float x, float y, float R, float r, float rot) {
        g.begin();
        for (int i = 0; i < 10; i++) {
            float a = rot - PI / 2 + i * PI / 5, rr = (i % 2 == 1) ? r : R;
            if (i == 0) g.moveTo(x + cos(a) * rr, y + sin(a) * rr); else g.lineTo(x + cos(a) * rr, y + sin(a) * rr);
        }
        g.close();
    }
    public static void heartPath(G g, float x, float y, float s) {
        g.begin(); g.moveTo(x, y + s * .35f);
        g.cubicTo(x - s * 1.1f, y - s * .35f, x - s * .45f, y - s * 1.05f, x, y - s * .45f);
        g.cubicTo(x + s * .45f, y - s * 1.05f, x + s * 1.1f, y - s * .35f, x, y + s * .35f);
        g.close();
    }
    static void tri(G g, float ax, float ay, float bx, float by, float cx, float cy, int col, float lw) {
        g.begin(); g.moveTo(ax, ay); g.lineTo(bx, by); g.lineTo(cx, cy); g.close();
        if (lw == 0) g.fill(col); else fs(g, col, lw);
    }

    /* ------------------------------------------------------------------ personagem */
    public static void drawChar(G g, Skin sk, Pose P, float t) {
        float d = P.drop;
        if (sk.wings) drawWings(g, d + P.bob, t, P.wing);
        if (sk.tail != null) drawTail(g, sk, d, t, P);
        if (!sk.hasHood) { ell(g, 0, -52 + d + P.bob, 24, 27, 0); fs(g, HAIR); }
        drawLeg(g, sk, -1, P.legL, P.lenL, d);
        drawLeg(g, sk, 1, P.legR, P.lenR, d);
        g.save(); g.translate(0, d + P.bob);
        drawTorso(g, sk);
        drawArm(g, sk, -1, P.hLx, P.hLy);
        drawArm(g, sk, 1, P.hRx, P.hRy);
        g.save(); g.translate(0, -60); g.rotate(P.tilt); g.translate(0, 60);
        drawHead(g, sk, P, t);
        g.restore();
        if (P.prop != null) drawProp(g, P.prop, P.hRx, P.hRy, t);
        g.restore();
    }

    static void drawLeg(G g, Skin sk, int side, float a, float len, float d) {
        float hx = side * 5.6f, hy = -21 + d, ex = hx + len * sin(a), ey = hy + len * cos(a);
        limb(g, hx, hy, ex, ey, 7, sk.pants);
        int col = side > 0 ? sk.shoeR : sk.shoeL;
        ell(g, ex + side * .8f, ey + 1.2f, 6, 3.9f, a * .5f); fs(g, WHITE, 1.4f);
        ell(g, ex + side * .8f, ey, 5.6f, 3.5f, a * .5f); g.fill(col);
        ell(g, ex + side * .8f, ey - .8f, 1.3f, 1.1f, 0); g.fill(0xD9FFFFFF);
    }

    static void drawTorso(G g, Skin sk) {
        if (sk.dress) {
            g.begin(); g.moveTo(-10, -45); g.quadTo(-13, -31, -21, -15);
            for (int i = 0; i < 5; i++) { float x0 = -21 + i * 8.4f; g.quadTo(x0 + 4.2f, -9.5f, x0 + 8.4f, -15); }
            g.quadTo(13, -31, 10, -45); g.quadTo(0, -48, -10, -45); g.close(); fs(g, sk.dressC);
            g.begin(); g.moveTo(-12, -33.5f); g.quadTo(0, -31, 12, -33.5f); g.stroke(sk.trim, 2.6f);
            float[][] st = {{-9, -22}, {3, -25}, {11, -19}, {-2, -18}};
            for (float[] p : st) { starPath(g, p[0], p[1], 1.8f, .8f, 0); g.fill(0xFFFFF6C9); }
            return;
        }
        g.begin(); g.moveTo(-11, -45); g.quadTo(-15, -34, -15.5f, -21); g.quadTo(-15.5f, -17, -11, -17); g.lineTo(11, -17);
        g.quadTo(15.5f, -17, 15.5f, -21); g.quadTo(15, -34, 11, -45); g.quadTo(0, -48, -11, -45); g.close(); fs(g, sk.jacket);
        if (sk.glow) { g.begin(); g.moveTo(-12.5f, -40); g.quadTo(-14, -30, -13.6f, -22); g.stroke(0x738CAAFF, 1.6f); }
        if (sk.belly != null) { ell(g, 0, -29, 8.2f, 9.5f, 0); g.fill(sk.belly); }
        if (sk.stripes != null) {
            for (float y : new float[]{-38, -31}) {
                g.begin(); g.moveTo(-14.6f, y); g.lineTo(-10.5f, y + 1.5f); g.stroke(sk.stripes, 2.2f);
                g.begin(); g.moveTo(14.6f, y); g.lineTo(10.5f, y + 1.5f); g.stroke(sk.stripes, 2.2f);
            }
        }
        if (sk.zip) {
            g.glow(sk.trim, 6); g.begin(); g.moveTo(0, -43); g.lineTo(0, -18); g.stroke(sk.trim, 1.6f); g.noGlow();
            g.begin(); g.moveTo(-11, -26); g.lineTo(-6, -26); g.moveTo(11, -26); g.lineTo(6, -26); g.stroke(0x9946EFFF, 1.2f);
        }
        g.begin(); g.moveTo(-15.4f, -20.5f); g.lineTo(15.4f, -20.5f); g.stroke(sk.trim, 1.8f);
    }

    static void drawArm(G g, Skin sk, int side, float hx, float hy) {
        float sx = side * 11.5f, sy = -41;
        limb(g, sx, sy, hx, hy, 6.4f, sk.sleeve);
        float dx = hx - sx, dy = hy - sy, L = (float) Math.hypot(dx, dy);
        if (L < 0.01f) L = 1;
        ell(g, hx - dx / L * 2.8f, hy - dy / L * 2.8f, 3.3f, 3.3f, 0); g.fill(sk.trim);
        ell(g, hx, hy, 3.3f, 3.3f, 0); fs(g, SKIN, 1.3f);
    }

    static void drawProp(G g, String prop, float hx, float hy, float t) {
        if ("star".equals(prop)) {
            g.glow(0xFFFFE066, 10); starPath(g, hx + 2, hy - 4, 6, 2.6f, sin(t * 2) * .2f); fs(g, 0xFFFFE066, 1.2f); g.noGlow();
        } else if ("wand".equals(prop)) {
            limb(g, hx, hy, hx + 7, hy - 13, 1.6f, 0xFFFFF2C2);
            g.glow(WHITE, 10); starPath(g, hx + 8, hy - 16, 5.5f, 2.4f, t * 1.5f); fs(g, 0xFFFFD36B, 1.2f); g.noGlow();
        }
    }

    static void drawWings(G g, float d, float t, float flap) {
        float k = .72f + .28f * sin(t * flap);
        g.save(); g.translate(0, -38 + d);
        for (int s = -1; s <= 1; s += 2) {
            g.save(); g.scale(s * k, 1);
            ell(g, 15, -10, 15, 10, -.6f); g.fill(0xD1BEF2FF); g.stroke(WHITE, 1.5f);
            ell(g, 12, 5, 10, 6.5f, .5f); g.fill(0xD9FFC4F0); g.stroke(WHITE, 1.5f);
            ell(g, 16, -12, 4, 2.4f, -.6f); g.fill(0xB3FFFFFF);
            g.restore();
        }
        g.restore();
    }

    static void drawTail(G g, Skin sk, float d, float t, Pose P) {
        float w = sin(t * 3) * .22f + P.tailW;
        g.save(); g.translate(-8, -22 + d); g.rotate(w);
        switch (sk.tail) {
            case "cat":
                g.begin(); g.moveTo(0, 0); g.cubicTo(-12, 3, -21, -3, -19, -17); g.stroke(OUT, 7.6f); g.stroke(sk.tailC, 5);
                ell(g, -19, -17, 2.6f, 2.6f, 0); g.fill(sk.stripes != null ? sk.stripes : WHITE);
                break;
            case "fox":
                ell(g, -13, -6, 13, 7.5f, -.6f); fs(g, sk.tailC);
                ell(g, -22, -12.5f, 5.5f, 4.2f, -.6f); g.fill(0xFFFFF4EA);
                break;
            case "bunny":
                ell(g, -4, 2, 5.6f, 5.6f, 0); fs(g, WHITE);
                break;
            case "dino":
                g.begin(); g.moveTo(1, -6); g.quadTo(-15, -5, -27, 5); g.quadTo(-13, 5, 1, 5); g.close(); fs(g, sk.tailC);
                for (float[] p : new float[][]{{-8, -5.5f}, {-16, -3}}) {
                    g.begin(); g.moveTo(p[0] - 3, p[1] + .8f); g.lineTo(p[0], p[1] - 4.5f); g.lineTo(p[0] + 3, p[1] + .2f); g.close();
                    g.fill(sk.trim); g.stroke(OUT, 1);
                }
                break;
            case "uni":
                for (int i = 0; i < 4; i++) {
                    g.begin(); g.moveTo(0, i * 1.2f - 2);
                    g.cubicTo(-9, 1 + i, -16, -4 + i * 2, -17 + i * 1.5f, -15 + i * 3.5f);
                    g.stroke(Skin.RAINBOW[i + 1], 3.2f);
                }
                break;
        }
        g.restore();
    }

    static void drawEarsBack(G g, Skin sk, float t) {
        int ec = sk.earC != 0 ? sk.earC : sk.hood, inn = sk.inner;
        if (sk.ears != null) for (int s = -1; s <= 1; s += 2) {
            switch (sk.ears) {
                case "cat":
                    tri(g, s * 8, -89, s * 23, -104, s * 26, -80, ec, 1.5f);
                    tri(g, s * 12, -88, s * 21.8f, -99, s * 22.6f, -84, inn, 0);
                    break;
                case "fox":
                    tri(g, s * 7, -87, s * 25, -111, s * 27, -78, ec, 1.5f);
                    tri(g, s * 11, -87, s * 23, -104, s * 23.5f, -83, inn, 0);
                    tri(g, s * 20.5f, -102.5f, s * 25, -111, s * 26.3f, -99, 0xFF34303F, 0);
                    break;
                case "bunny": {
                    float r = s * .16f + sin(t * 3 + s) * .05f;
                    ell(g, s * 10, -101, 6.6f, 17, r); fs(g, ec);
                    ell(g, s * 10, -100, 3.2f, 12, r); g.fill(inn);
                    break;
                }
                case "bear":
                    ell(g, s * 20.5f, -88.5f, 8.6f, 8.6f, 0); fs(g, ec);
                    ell(g, s * 20.5f, -88.5f, 4.4f, 4.4f, 0); g.fill(inn);
                    break;
                case "uni":
                    tri(g, s * 12, -90, s * 22, -101, s * 25, -83, ec, 1.5f);
                    tri(g, s * 15, -89, s * 21, -97.5f, s * 22.6f, -86, inn, 0);
                    break;
                default:
                    break;
            }
        }
        if ("dino".equals(sk.ears)) {
            for (int k = 0; k < 5; k++) {
                float a = -2.55f + k * .4f, bx = cos(a) * 27, by = -66 + sin(a) * 26.5f, ox = cos(a) * 10, oy = sin(a) * 10,
                        px = -sin(a) * 4.5f, py = cos(a) * 4.5f;
                g.begin(); g.moveTo(bx - px, by - py); g.lineTo(bx + ox, by + oy); g.lineTo(bx + px, by + py); g.close();
                fs(g, sk.trim, 1.3f);
            }
        }
        if (sk.mane) {
            float[][] ms = {{-8, -93, 7}, {-1, -96, 6.5f}, {-14, -88, 6}, {24, -80, 6.5f}, {27, -70, 6}, {27.5f, -60, 5.5f}};
            for (int i = 0; i < ms.length; i++) { ell(g, ms[i][0], ms[i][1], ms[i][2], ms[i][2], 0); fs(g, Skin.RAINBOW[i % 6], 1.2f); }
        }
    }

    static void drawHead(G g, Skin sk, Pose P, float t) {
        float f = P.fx;
        if (sk.hasHood) {
            drawEarsBack(g, sk, t);
            ell(g, 0, -66, 28, 27.5f, 0); fs(g, sk.hood);
            g.begin(); g.arc(0, -66, 24.5f, -2.75f, -1.75f); g.stroke(sk.glow ? 0x8C8CAAFF : 0x73FFFFFF, 2.6f);
            if (sk.stripes != null) {
                for (float x : new float[]{-8, 0, 8}) {
                    g.begin(); g.moveTo(x, -93 + Math.abs(x) * .15f); g.lineTo(x * .8f, -86); g.stroke(sk.stripes, 2.6f);
                }
            }
            ell(g, f * .6f, -60.5f, 21.5f, 22, 0); g.fill(HAIR);
        } else {
            ell(g, 0, -66, 25, 25.5f, 0); fs(g, HAIR);
        }
        for (int s = -1; s <= 1; s += 2) {
            float sw = P.hair * s, o = f * .4f;
            g.begin(); g.moveTo(s * 12.5f + o, -68); g.quadTo(s * 22 + o, -66, s * 21.5f + o, -57);
            g.quadTo(s * 23.5f + o, -49, s * 20 + o + sw, -37); g.quadTo(s * 17 + o, -43, s * 15.5f + o, -49);
            g.quadTo(s * 13 + o, -58, s * 12.5f + o, -68); g.close(); fs(g, HAIR, 1.2f);
            g.begin(); g.moveTo(s * 18.5f + o, -60); g.quadTo(s * 20.5f + o, -52, s * 18.5f + o + sw * .5f, -44); g.stroke(HAIR_HI, 1.3f);
        }
        ell(g, f * .8f, -56.5f, 17.5f, 16.4f, 0); fs(g, SKIN, 1.25f);
        g.begin(); g.moveTo(f - 18, -55); g.quadTo(f - 19.5f, -77, f, -78); g.quadTo(f + 19.5f, -77, f + 18.5f, -55);
        g.quadTo(f + 15.5f, -63, f + 10.5f, -63); g.quadTo(f + 7.5f, -67.5f, f + 2.5f, -64.5f); g.quadTo(f - 2.5f, -69, f - 7, -64);
        g.quadTo(f - 12, -67.5f, f - 14, -61.5f); g.quadTo(f - 16, -59.5f, f - 18, -55); g.close(); fs(g, HAIR, 1.2f);
        g.begin(); g.moveTo(f - 9, -73); g.quadTo(f - 2, -76, f + 6, -73.5f); g.stroke(HAIR_HI, 1.8f);
        g.save(); g.mulAlpha(P.blush);
        ell(g, f - 11.2f, -49.5f, 3.7f, 2.3f, 0); g.fill(0xFFFF8FA8);
        ell(g, f + 12.6f, -49.5f, 3.7f, 2.3f, 0); g.fill(0xFFFF8FA8);
        g.restore();
        drawEyes(g, P.eyes, f);
        drawGlasses(g, f);
        drawMouth(g, P.mouth, f + .6f, -45.8f);
        if (sk.hasHood) {
            ell(g, f * .6f, -60.5f, 21.5f, 22, 0);
            if (sk.glow) g.glow(sk.trim, 9);
            g.stroke(sk.trim, 2.8f);
            if (sk.glow) g.noGlow();
            if (sk.patch) { ell(g, -17, -80, 4, 2.6f, -.5f); g.fill(0xE62C2C36); ell(g, 17, -80, 4, 2.6f, .5f); g.fill(0xE62C2C36); }
            if (sk.horn) {
                g.begin(); g.moveTo(-5, -90); g.lineTo(1.5f, -115); g.lineTo(5.5f, -90); g.close();
                g.glow(0xFFFFF3B0, 8); fs(g, sk.trim, 1.4f); g.noGlow();
                for (float y : new float[]{-95, -101, -107}) {
                    g.begin(); g.moveTo(-3 + (y + 90) * -.08f, y + 2); g.lineTo(4 + (y + 90) * -.06f, y - 1); g.stroke(0xFFE0A72C, 1.2f);
                }
            }
        } else if (sk.crown) {
            int[] cols = {0xFFFF9FCF, 0xFFFFFFFF, 0xFFC9A2FF, 0xFFFFE27A, 0xFFFF9FCF};
            for (int i = 0; i < 5; i++) {
                float a = -2.5f + i * .4f, x = cos(a) * 24.5f, y = -66 + sin(a) * 25;
                for (int k = 0; k < 5; k++) {
                    ell(g, x + cos(k * 1.256f) * 2.5f, y + sin(k * 1.256f) * 2.5f, 2.2f, 2.2f, 0); g.fill(cols[i]); g.stroke(OUT, .6f);
                }
                ell(g, x, y, 1.4f, 1.4f, 0); g.fill(0xFFFFD34D);
            }
        }
    }

    static void drawEyes(G g, int e, float f) {
        float y = -54.5f;
        float[] xs = {f - 7.4f, f + 8.6f};
        for (int i = 0; i < 2; i++) {
            float x = xs[i];
            switch (e) {
                case Pose.E_WIDE:
                    ell(g, x, y, 4.3f, 5.5f, 0); g.fill(EYE);
                    ell(g, x + 1.3f, y - 2, 1.8f, 1.8f, 0); g.fill(WHITE);
                    ell(g, x - 1.2f, y + 2.2f, .9f, .9f, 0); g.fill(WHITE);
                    break;
                case Pose.E_BLINK:
                    g.begin(); g.moveTo(x - 3.5f, y + .8f); g.lineTo(x + 3.5f, y + .8f); g.stroke(EYE, 1.9f);
                    break;
                case Pose.E_HAPPY:
                    g.begin(); g.moveTo(x - 3.7f, y + 1.6f); g.quadTo(x, y - 4.6f, x + 3.7f, y + 1.6f); g.stroke(EYE, 1.9f);
                    break;
                case Pose.E_CLOSED:
                    g.begin(); g.moveTo(x - 3.7f, y - .4f); g.quadTo(x, y + 3.6f, x + 3.7f, y - .4f); g.stroke(EYE, 1.9f);
                    break;
                case Pose.E_SQUINT: {
                    float s = i == 1 ? -1 : 1;
                    g.begin(); g.moveTo(x - 3 * s, y - 3.2f); g.lineTo(x + 2.8f * s, y); g.lineTo(x - 3 * s, y + 3.2f); g.stroke(EYE, 1.9f);
                    break;
                }
                case Pose.E_DIZZY:
                    g.begin();
                    for (int k = 0; k < 22; k++) {
                        float a = k * .62f, r = k * .2f;
                        if (k == 0) g.moveTo(x, y); else g.lineTo(x + cos(a) * r, y + sin(a) * r);
                    }
                    g.stroke(EYE, 1.3f);
                    break;
                default:
                    ell(g, x, y, 3.6f, 4.7f, 0); g.fill(EYE);
                    ell(g, x + 1.1f, y - 1.7f, 1.4f, 1.4f, 0); g.fill(WHITE);
                    ell(g, x - 1, y + 1.9f, .7f, .7f, 0); g.fill(WHITE);
            }
        }
    }

    static void drawGlasses(G g, float f) {
        float y = -54.8f, x0 = f - 7.4f, x1 = f + 8.6f;
        for (float x : new float[]{x0, x1}) { ell(g, x, y, 6.5f, 6.1f, 0); g.fill(0x1FFFFFFF); g.stroke(GLASS, 1.8f); }
        g.begin(); g.moveTo(x0 + 6.4f, y - .8f); g.quadTo((x0 + x1) / 2, y - 3, x1 - 6.4f, y - .8f); g.stroke(GLASS, 1.8f);
        g.begin(); g.moveTo(x0 - 6.4f, y - 1); g.lineTo(f - 17.3f, y - 2.5f); g.moveTo(x1 + 6.4f, y - 1); g.lineTo(f + 17.8f, y - 2.5f);
        g.stroke(GLASS, 1.8f);
    }

    static void drawMouth(G g, int m, float x, float y) {
        switch (m) {
            case Pose.M_GRIN:
                g.begin(); g.moveTo(x - 4.2f, y - 1.4f); g.quadTo(x, y + 5.6f, x + 4.2f, y - 1.4f); g.close(); fs(g, MOUTH, 1.2f);
                ell(g, x, y + 2.2f, 2, 1.3f, 0); g.fill(TONGUE);
                break;
            case Pose.M_OPEN:
                ell(g, x, y + .8f, 2.8f, 3.5f, 0); fs(g, MOUTH, 1.2f);
                ell(g, x, y + 2.6f, 1.8f, 1.1f, 0); g.fill(TONGUE);
                break;
            case Pose.M_O:
                ell(g, x, y + .5f, 1.8f, 2.1f, 0); fs(g, MOUTH, 1);
                break;
            case Pose.M_FLAT:
                g.begin(); g.moveTo(x - 2.5f, y); g.lineTo(x + 2.5f, y); g.stroke(EYE, 1.6f);
                break;
            case Pose.M_WAVY:
                g.begin(); g.moveTo(x - 4, y);
                for (int i = 1; i <= 4; i++) g.lineTo(x - 4 + i * 2, y + (i % 2 == 1 ? 1.6f : -.4f));
                g.stroke(EYE, 1.6f);
                break;
            default:
                g.begin(); g.moveTo(x - 3.1f, y - .7f); g.quadTo(x, y + 2.8f, x + 3.1f, y - .7f); g.stroke(EYE, 1.6f);
        }
    }

    /* ------------------------------------------------------------------ escada e corda */
    public static void drawLadder(G g, float x, float yTop, float yBot, float S, float alpha) {
        if (yBot - yTop < 2) return;
        g.save(); g.mulAlpha(alpha);
        float hw = 7.5f * S;
        for (int pass = 0; pass < 2; pass++) {
            int col = pass == 0 ? OUT : 0xFFE0A869;
            float wr = pass == 0 ? 4.6f * S : 2.8f * S, wg = pass == 0 ? 3.8f * S : 2.2f * S;
            for (float y = yBot - 7 * S; y > yTop + 3 * S; y -= 13 * S) {
                g.begin(); g.moveTo(x - hw, y); g.lineTo(x + hw, y); g.stroke(col, wg);
            }
            g.begin(); g.moveTo(x - hw, yBot); g.lineTo(x - hw, yTop); g.moveTo(x + hw, yBot); g.lineTo(x + hw, yTop);
            g.stroke(pass == 0 ? OUT : 0xFFC98A4B, wr);
        }
        g.restore();
    }

    public static void drawRope(G g, float x, float yTop, float yBot, float S, float sway, float alpha) {
        if (yBot - yTop < 2) return;
        g.save(); g.mulAlpha(alpha);
        g.begin(); g.moveTo(x, yTop); g.quadTo(x + sway, (yTop + yBot) / 2, x + sway * .6f, yBot);
        g.stroke(OUT, 3.8f * S); g.stroke(0xFFE8C07A, 2.4f * S);
        g.begin(); g.moveTo(x - 4 * S, yTop - 1 * S); g.lineTo(x + 4 * S, yTop - 1 * S); g.stroke(OUT, 4.2f * S); g.stroke(0xFF9B6A3A, 2.6f * S);
        ell(g, x, yTop + 2.5f * S, 3 * S, 2.6f * S, 0); g.fill(0xFFD9A55A); g.stroke(OUT, 1.2f * S);
        g.restore();
    }

    /* ------------------------------------------------------------------ cenários */
    public static void paintScene(G g, float w, float h, String name) {
        Random r = new Random(7);
        float m = Math.min(w, h);
        if ("dia".equals(name)) {
            g.begin(); g.rect(0, 0, w, h); g.fillLinear(0, 0, 0, h, new int[]{0xFF74C5FF, 0xFFDFF5FF}, new float[]{0, 1});
            float sx = w * .14f, sy = h * .16f, sr = m * .07f;
            g.begin(); g.rect(sx - sr * 3, sy - sr * 3, sr * 6, sr * 6);
            g.fillRadial(sx, sy, sr * 2.6f, new int[]{0x99FFEC8C, 0x99FFEC8C, 0x00FFEC8C}, new float[]{0, .3f, 1});
            ell(g, sx, sy, sr, sr, 0); g.fill(0xFFFFE066);
            for (int i = 0; i < 5; i++) {
                float cx = r.nextFloat() * w, cy = h * (.1f + r.nextFloat() * .35f), cs = m * (.05f + r.nextFloat() * .05f);
                float[][] bl = {{0, 0, 1}, {1, .15f, .8f}, {-1, .2f, .75f}, {.5f, -.4f, .8f}, {-.45f, -.3f, .7f}};
                for (float[] b : bl) { ell(g, cx + b[0] * cs, cy + b[1] * cs, b[2] * cs, b[2] * cs, 0); g.fill(0xF2FFFFFF); }
            }
            g.begin(); g.moveTo(0, h); g.lineTo(0, h * .8f); g.cubicTo(w * .25f, h * .7f, w * .45f, h * .86f, w * .7f, h * .76f);
            g.cubicTo(w * .85f, h * .71f, w, h * .78f, w, h * .78f); g.lineTo(w, h); g.close(); g.fill(0xFFA5E589);
            g.begin(); g.moveTo(0, h); g.lineTo(0, h * .9f); g.cubicTo(w * .3f, h * .84f, w * .6f, h * .94f, w, h * .86f); g.lineTo(w, h); g.close();
            g.fill(0xFF6FCB5E);
            int[] fc = {0xFFFFFFFF, 0xFFFFD1E8, 0xFFFFE066, 0xFFC9A2FF};
            for (int i = 0; i < w / 18; i++) {
                float fx = r.nextFloat() * w, fy = h * (.9f + r.nextFloat() * .09f);
                ell(g, fx, fy, m * .006f + 1, m * .006f + 1, 0); g.fill(fc[r.nextInt(4)]);
            }
        } else if ("quarto".equals(name)) {
            g.begin(); g.rect(0, 0, w, h); g.fill(0xFFFFE6F1);
            float st = m * .08f;
            int row = 0;
            for (float y = st / 2; y < h; y += st, row++)
                for (float x = (row % 2 == 1) ? st / 2 : st; x < w; x += st) { ell(g, x, y, m * .008f, m * .008f, 0); g.fill(0xFFFFD3E6); }
            float wx = w * .58f, wy = h * .12f, ww = m * .42f, wh = m * .32f;
            g.begin(); g.rect(wx - 8, wy - 8, ww + 16, wh + 16); g.fill(WHITE);
            g.begin(); g.rect(wx, wy, ww, wh); g.fillLinear(0, wy, 0, wy + wh, new int[]{0xFF8FD3FF, 0xFFD9F3FF}, new float[]{0, 1});
            g.begin(); g.rect(wx + ww / 2 - 3, wy, 6, wh); g.rect(wx, wy + wh / 2 - 3, ww, 6); g.fill(WHITE);
            g.begin(); g.rect(wx - m * .06f, wy - 12, m * .06f, wh * 1.25f); g.rect(wx + ww, wy - 12, m * .06f, wh * 1.25f); g.fill(0xFFF7A6C9);
            g.begin(); g.rect(w * .1f, h * .38f, m * .34f, m * .025f); g.fill(0xFFE7B483);
            int[] bc = {0xFFA06BFF, 0xFF46C7FF, 0xFFFF86B6};
            for (int i = 0; i < 3; i++) { g.begin(); g.rect(w * .1f + m * (.03f + i * .07f), h * .38f - m * .09f, m * .05f, m * .09f); g.fill(bc[i]); }
            float fh = m * .09f;
            g.begin(); g.rect(0, h - fh, w, fh); g.fill(0xFFF0C08E);
            g.begin(); for (float x = 0; x < w; x += m * .18f) { g.moveTo(x, h - fh); g.lineTo(x, h); } g.stroke(0x40A06432, 2);
            ell(g, w * .5f, h - fh * .45f, m * .45f, fh * .35f, 0); g.fill(0x59C896FF);
        } else {
            g.begin(); g.rect(0, 0, w, h);
            g.fillLinear(0, 0, 0, h, new int[]{0xFF0A0F2E, 0xFF1B2566, 0xFF35307C}, new float[]{0, .6f, 1});
            int n = (int) (w * h / 2600);
            for (int i = 0; i < n; i++) {
                float a = r.nextFloat() * .7f + .2f;
                g.begin(); g.rect(r.nextFloat() * w, r.nextFloat() * h * .75f, r.nextFloat() * 1.8f + .4f, r.nextFloat() * 1.8f + .4f);
                g.fill(((int) (a * 255) << 24) | 0xFFFFFF);
            }
            float mx = w * .82f, my = h * .17f, mr = m * .065f;
            g.begin(); g.rect(mx - mr * 4, my - mr * 4, mr * 8, mr * 8);
            g.fillRadial(mx, my, mr * 3.2f, new int[]{0x73FFF0BE, 0x73FFF0BE, 0x00FFF0BE}, new float[]{0, .16f, 1});
            ell(g, mx, my, mr, mr, 0); g.fill(0xFFFFF3C4);
            int[][] layers = {{0xFF161F52, 16, 34, 35}, {0xFF0F1640, 9, 22, 60}};
            for (int[] L : layers) {
                float x = -10;
                while (x < w) {
                    float bw = m * (.07f + r.nextFloat() * .08f), bh = h * (L[1] / 100f + r.nextFloat() * (L[2] - L[1]) / 100f);
                    g.begin(); g.rect(x, h - bh, bw, bh); g.fill(L[0]);
                    if (r.nextFloat() < .3f) { g.begin(); g.rect(x + bw * .4f, h - bh - m * .03f, bw * .12f, m * .03f); g.fill(L[0]); }
                    for (float yy = h - bh + m * .02f; yy < h - m * .03f; yy += m * .03f)
                        for (float xx = x + m * .012f; xx < x + bw - m * .02f; xx += m * .026f)
                            if (r.nextFloat() < L[3] / 200f) {
                                g.begin(); g.rect(xx, yy, m * .011f, m * .014f);
                                g.fill(r.nextFloat() < .5f ? 0xD9FFD678 : 0xB378EBFF);
                            }
                    x += bw + m * .004f;
                }
            }
            g.begin(); g.rect(0, h - m * .05f, w, m * .05f);
            g.fillLinear(0, h - m * .05f, 0, h, new int[]{0x0046EFFF, 0x4046EFFF}, new float[]{0, 1});
        }
    }
}
