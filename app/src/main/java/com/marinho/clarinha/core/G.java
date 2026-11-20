package com.marinho.clarinha.core;

/** Superfície de desenho mínima, usada pelo app e pelos testes. */
public interface G {
    void save();
    void restore();
    void translate(float x, float y);
    void rotate(float rad);
    void scale(float sx, float sy);
    /** Multiplica a opacidade atual (desfeito no restore). */
    void mulAlpha(float a);

    void begin();
    void moveTo(float x, float y);
    void lineTo(float x, float y);
    void quadTo(float cx, float cy, float x, float y);
    void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y);
    void close();
    void ellipse(float cx, float cy, float rx, float ry, float rot);
    void arc(float cx, float cy, float r, float a0, float a1);
    void rect(float x, float y, float w, float h);
    void roundRect(float x, float y, float w, float h, float r);

    void fill(int argb);
    void stroke(int argb, float width);
    void fillLinear(float x0, float y0, float x1, float y1, int[] colors, float[] stops);
    void fillRadial(float cx, float cy, float r, int[] colors, float[] stops);
    void glow(int argb, float blurPx);
    void noGlow();
    void dash(boolean on, float len);

    void text(String s, float x, float y, float size, int fill, int strokeCol, float strokeW);
    float textWidth(String s, float size);
}
