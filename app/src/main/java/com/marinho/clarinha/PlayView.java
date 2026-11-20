package com.marinho.clarinha;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import com.marinho.clarinha.core.Painter;
import com.marinho.clarinha.core.Pet;
import com.marinho.clarinha.core.Render;
import com.marinho.clarinha.core.World;

import java.util.Random;

/** Tela cheia do app: cenário + personagens. */
public final class PlayView extends View implements Choreographer.FrameCallback {
    public interface Listener { void onSelect(Pet p); }

    public final World world = new World();
    private final AndroidG g = new AndroidG();
    private Bitmap sceneBmp;
    private String scene = "noite";
    private float[][] twinkles = new float[0][];
    private long lastNs;
    private boolean running;
    public Pet selected;
    public boolean showSelected;
    public Listener listener;
    private final float density;
    private boolean sized;
    private int[] pendingTeam;

    public PlayView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        world.overlay = false;
        world.allowStars = true;
    }

    public void setScene(String s) { scene = s; buildScene(); invalidate(); }
    public String scene() { return scene; }

    public void setTeam(int[] skins) {
        if (!sized) { pendingTeam = skins; return; }
        world.pets.clear();
        for (int i = 0; i < skins.length; i++) world.addPet(skins[i], world.W * (i + 1) / (skins.length + 1));
        selected = world.pets.isEmpty() ? null : world.pets.get(0);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float m = Math.min(w, h);
        float ch = World.clamp(m * .23f, 80 * density, 200 * density);
        world.setSize(w, h, ch, h - Math.round(m * .03f), 0, density);
        Random r = new Random();
        twinkles = new float[30][];
        for (int i = 0; i < 30; i++) twinkles[i] = new float[]{r.nextFloat() * w, r.nextFloat() * h * .6f, r.nextFloat() * 6.28f, (.8f + r.nextFloat() * 1.2f) * density};
        buildScene();
        sized = true;
        if (pendingTeam != null) { int[] t = pendingTeam; pendingTeam = null; setTeam(t); }
    }

    private void buildScene() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (sceneBmp == null || sceneBmp.getWidth() != w || sceneBmp.getHeight() != h) {
            if (sceneBmp != null) sceneBmp.recycle();
            sceneBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        Canvas c = new Canvas(sceneBmp);
        Painter.paintScene(g.set(c), w, h, scene);
    }

    public void start() { if (!running) { running = true; lastNs = 0; Choreographer.getInstance().postFrameCallback(this); } }
    public void stop() { running = false; Choreographer.getInstance().removeFrameCallback(this); }

    @Override public void doFrame(long ns) {
        if (!running) return;
        float dt = lastNs == 0 ? .016f : (ns - lastNs) / 1e9f;
        lastNs = ns;
        world.step(dt);
        world.checkHold(android.os.SystemClock.uptimeMillis() / 1000f);
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (sceneBmp != null) canvas.drawBitmap(sceneBmp, 0, 0, null);
        g.set(canvas);
        if ("noite".equals(scene)) Render.drawTwinkles(g, world, twinkles);
        Render.drawStars(g, world);
        for (Pet p : world.pets) if (!"held".equals(p.state)) Render.drawPet(g, world, p, true, showSelected && p == selected);
        for (Pet p : world.pets) if ("held".equals(p.state)) Render.drawPet(g, world, p, true, showSelected && p == selected);
        Render.drawParts(g, world, 0);
        for (Pet p : world.pets) Render.drawBubble(g, world, p);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        float tm = e.getEventTime() / 1000f;
        switch (a) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = e.getActionIndex();
                Pet p = world.pointerDown(e.getPointerId(i), e.getX(i), e.getY(i), tm);
                if (p != null) { selected = p; if (listener != null) listener.onSelect(p); }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) world.pointerMove(e.getPointerId(i), e.getX(i), e.getY(i), tm);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = e.getActionIndex();
                world.pointerUp(e.getPointerId(i), e.getX(i), e.getY(i), tm, false);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < e.getPointerCount(); i++) world.pointerUp(e.getPointerId(i), e.getX(i), e.getY(i), tm, true);
                break;
            default: break;
        }
        return true;
    }
}
