package com.marinho.clarinha;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.marinho.clarinha.core.Painter;

import java.util.ArrayList;
import java.util.List;

/** Tela de ajuste: marca, por cima da tela inicial, os ícones onde ela pode brincar. */
final class SetupView extends View {
    private final PetService svc;
    private final Runnable done;
    private final AndroidG g = new AndroidG();
    private final ArrayList<float[]> pts = new ArrayList<>();
    private final int[] loc = new int[2];
    private final float d;
    private float iconDp, downX, downY;
    private boolean cardBottom;
    private final RectF card = new RectF();
    private final RectF[] btn = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
    private static final String[] LABELS = {"−", "+", "Limpar", "↕", "Pronto"};
    private static final float[] WEIGHTS = {1, 1, 1.7f, 1, 1.9f};
    private static final String[] LINES = {
            "Toque em cima dos ícones onde",
            "a turminha pode brincar.",
            "Toque de novo para desmarcar.",
            "Use − e + até o quadrado ficar",
            "do tamanho do ícone."};

    SetupView(PetService svc, Runnable done) {
        super(svc);
        this.svc = svc;
        this.done = done;
        d = svc.getResources().getDisplayMetrics().density;
        reload();
    }

    void reload() {
        pts.clear();
        for (float[] c : Prefs.icons(svc)) pts.add(new float[]{c[0] * svc.screenW, c[1] * svc.screenH});
        iconDp = Prefs.iconDp(svc);
        invalidate();
    }

    private void layoutCard() {
        float W = svc.screenW, H = svc.screenH;
        float cw = Math.min(W - 24 * d, 460 * d), lineH = 21 * d, ch = 16 * d + LINES.length * lineH + 12 * d + 48 * d + 14 * d;
        float x = (W - cw) / 2, y = cardBottom ? H - svc.bottomInset - ch - 16 * d : svc.topInset + 16 * d;
        card.set(x, y, x + cw, y + ch);
        float bx = x + 12 * d, by = y + ch - 14 * d - 48 * d, gap = 8 * d, tot = 0;
        for (float w : WEIGHTS) tot += w;
        float unit = (cw - 24 * d - gap * (btn.length - 1)) / tot;
        for (int i = 0; i < btn.length; i++) {
            float bw = unit * WEIGHTS[i];
            btn[i].set(bx, by, bx + bw, by + 48 * d);
            bx += bw + gap;
        }
    }

    @Override protected void onDraw(Canvas c) {
        getLocationOnScreen(loc);
        c.translate(-loc[0], -loc[1]);
        g.set(c);
        float W = svc.screenW, H = svc.screenH, size = iconDp * d;
        g.begin(); g.rect(-50, -50, W + 100, H + 100); g.fill(0x8C140A2E);
        for (float[] p : pts) {
            g.begin(); g.roundRect(p[0] - size / 2, p[1] - size / 2, size, size, size * .22f);
            g.fill(0x40FFE066); g.stroke(0xFFFFE066, 3 * d);
            Painter.starPath(g, p[0], p[1] - size / 2, 7 * d, 3 * d, 0); g.fill(0xFFFFE066); g.stroke(0xFF2D1B42, 1.2f * d);
        }
        layoutCard();
        g.begin(); g.roundRect(card.left, card.top, card.width(), card.height(), 20 * d); g.fill(0xF7FFF7FB); g.stroke(0xFFA06BFF, 2 * d);
        float ty = card.top + 16 * d + 10 * d;
        for (int i = 0; i < LINES.length; i++) {
            g.text(LINES[i], card.centerX(), ty + i * 21 * d, 15 * d, i < 2 ? 0xFF3A2558 : 0xFF6B5590, 0, 0);
        }
        for (int i = 0; i < btn.length; i++) {
            RectF r = btn[i];
            int bg = i == 4 ? 0xFFA06BFF : (i == 2 ? 0xFFFFE1EE : 0xFFF0E3FF), fg = i == 4 ? 0xFFFFFFFF : (i == 2 ? 0xFFA2376B : 0xFF5A3A8A);
            g.begin(); g.roundRect(r.left, r.top, r.width(), r.height(), 14 * d); g.fill(bg);
            g.text(LABELS[i], r.centerX(), r.centerY(), (i < 2 || i == 3 ? 22 : 16) * d, fg, 0, 0);
        }
        g.text(Math.round(iconDp) + "", btn[1].right - 9 * d, btn[1].top + 9 * d, 10 * d, 0xFF8A6BB5, 0, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) { downX = e.getRawX(); downY = e.getRawY(); }
        else if (a == MotionEvent.ACTION_UP) {
            float x = e.getRawX(), y = e.getRawY();
            if (Math.hypot(x - downX, y - downY) < 14 * d) tap(x, y);
        }
        return true;
    }

    private void tap(float x, float y) {
        for (int i = 0; i < btn.length; i++) if (btn[i].contains(x, y)) { press(i); return; }
        if (card.contains(x, y)) return;
        float half = iconDp * d / 2;
        for (int i = 0; i < pts.size(); i++) {
            float[] p = pts.get(i);
            if (Math.abs(p[0] - x) < half && Math.abs(p[1] - y) < half) { pts.remove(i); invalidate(); return; }
        }
        pts.add(new float[]{x, y});
        invalidate();
    }

    private void press(int i) {
        switch (i) {
            case 0: iconDp = Math.max(32, iconDp - 4); break;
            case 1: iconDp = Math.min(120, iconDp + 4); break;
            case 2: pts.clear(); break;
            case 3: cardBottom = !cardBottom; break;
            case 4: {
                List<float[]> l = new ArrayList<>();
                for (float[] p : pts) l.add(new float[]{p[0] / svc.screenW, p[1] / svc.screenH});
                Prefs.setIcons(svc, l);
                Prefs.setIconDp(svc, iconDp);
                done.run();
                return;
            }
            default: break;
        }
        invalidate();
    }
}
