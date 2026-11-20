package com.marinho.clarinha;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.marinho.clarinha.core.G;

/** Implementação da superfície de desenho sobre o Canvas do Android. */
public final class AndroidG implements G {
    private Canvas c;
    private final Path path = new Path(), tmp = new Path();
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG), strokeP = new Paint(Paint.ANTI_ALIAS_FLAG), textP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Matrix m = new Matrix();
    private float alpha = 1;
    private float[] aStack = new float[64];
    private int ai;
    private boolean glowOn;
    private int glowCol;
    private float glowBlur;

    public AndroidG() {
        fillP.setStyle(Paint.Style.FILL);
        strokeP.setStyle(Paint.Style.STROKE);
        strokeP.setStrokeCap(Paint.Cap.ROUND);
        strokeP.setStrokeJoin(Paint.Join.ROUND);
        textP.setTextAlign(Paint.Align.CENTER);
        textP.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public AndroidG set(Canvas canvas) {
        c = canvas; ai = 0; alpha = 1; glowOn = false;
        fillP.clearShadowLayer(); strokeP.clearShadowLayer(); strokeP.setPathEffect(null);
        return this;
    }

    private int mul(int argb) {
        int a = Math.round(((argb >>> 24) & 255) * alpha);
        if (a < 0) a = 0; else if (a > 255) a = 255;
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @Override public void save() {
        c.save();
        if (ai >= aStack.length) aStack = java.util.Arrays.copyOf(aStack, aStack.length * 2);
        aStack[ai++] = alpha;
    }
    @Override public void restore() { c.restore(); if (ai > 0) alpha = aStack[--ai]; }
    @Override public void translate(float x, float y) { c.translate(x, y); }
    @Override public void rotate(float rad) { c.rotate((float) Math.toDegrees(rad)); }
    @Override public void scale(float sx, float sy) { c.scale(sx, sy); }
    @Override public void mulAlpha(float a) { alpha *= a; }

    @Override public void begin() { path.reset(); }
    @Override public void moveTo(float x, float y) { path.moveTo(x, y); }
    @Override public void lineTo(float x, float y) { path.lineTo(x, y); }
    @Override public void quadTo(float cx, float cy, float x, float y) { path.quadTo(cx, cy, x, y); }
    @Override public void cubicTo(float a, float b, float cc, float d, float x, float y) { path.cubicTo(a, b, cc, d, x, y); }
    @Override public void close() { path.close(); }

    @Override public void ellipse(float cx, float cy, float rx, float ry, float rot) {
        rx = Math.abs(rx); ry = Math.abs(ry);
        tmp.reset(); rf.set(cx - rx, cy - ry, cx + rx, cy + ry); tmp.addOval(rf, Path.Direction.CW);
        if (rot != 0) { m.setRotate((float) Math.toDegrees(rot), cx, cy); tmp.transform(m); }
        path.addPath(tmp);
    }

    @Override public void arc(float cx, float cy, float r, float a0, float a1) {
        if (Math.abs(a1 - a0) >= Math.PI * 2 - 1e-3) { path.addCircle(cx, cy, r, Path.Direction.CW); return; }
        rf.set(cx - r, cy - r, cx + r, cy + r);
        path.arcTo(rf, (float) Math.toDegrees(a0), (float) Math.toDegrees(a1 - a0), true);
    }

    @Override public void rect(float x, float y, float w, float h) { path.addRect(x, y, x + w, y + h, Path.Direction.CW); }
    @Override public void roundRect(float x, float y, float w, float h, float r) {
        rf.set(x, y, x + w, y + h); path.addRoundRect(rf, r, r, Path.Direction.CW);
    }

    private void applyGlow(Paint p) {
        if (glowOn) p.setShadowLayer(glowBlur, 0, 0, mul(glowCol)); else p.clearShadowLayer();
    }

    @Override public void fill(int argb) {
        fillP.setShader(null); fillP.setColor(mul(argb)); applyGlow(fillP); c.drawPath(path, fillP);
    }

    @Override public void stroke(int argb, float width) {
        strokeP.setColor(mul(argb)); strokeP.setStrokeWidth(width); applyGlow(strokeP); c.drawPath(path, strokeP);
    }

    private int[] mulAll(int[] cols) { int[] r = new int[cols.length]; for (int i = 0; i < cols.length; i++) r[i] = mul(cols[i]); return r; }

    @Override public void fillLinear(float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
        fillP.setShader(new LinearGradient(x0, y0, x1, y1, mulAll(colors), stops, Shader.TileMode.CLAMP));
        fillP.setColor(0xFF000000); fillP.clearShadowLayer(); c.drawPath(path, fillP); fillP.setShader(null);
    }

    @Override public void fillRadial(float cx, float cy, float r, int[] colors, float[] stops) {
        fillP.setShader(new RadialGradient(cx, cy, Math.max(1, r), mulAll(colors), stops, Shader.TileMode.CLAMP));
        fillP.setColor(0xFF000000); fillP.clearShadowLayer(); c.drawPath(path, fillP); fillP.setShader(null);
    }

    @Override public void glow(int argb, float blurPx) { glowOn = true; glowCol = argb; glowBlur = Math.max(.5f, blurPx); }
    @Override public void noGlow() { glowOn = false; fillP.clearShadowLayer(); strokeP.clearShadowLayer(); }
    @Override public void dash(boolean on, float len) {
        strokeP.setPathEffect(on ? new DashPathEffect(new float[]{len, len * .8f}, 0) : null);
    }

    @Override public void text(String s, float x, float y, float size, int fill, int strokeCol, float strokeW) {
        textP.setTextSize(size);
        Paint.FontMetrics fm = textP.getFontMetrics();
        float by = y - (fm.ascent + fm.descent) / 2;
        if (strokeW > 0) {
            textP.setStyle(Paint.Style.STROKE); textP.setStrokeWidth(strokeW); textP.setStrokeJoin(Paint.Join.ROUND);
            textP.setColor(mul(strokeCol)); c.drawText(s, x, by, textP);
        }
        textP.setStyle(Paint.Style.FILL); textP.setColor(mul(fill)); c.drawText(s, x, by, textP);
    }

    @Override public float textWidth(String s, float size) { textP.setTextSize(size); return textP.measureText(s); }
}
