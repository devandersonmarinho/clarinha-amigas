package com.marinho.clarinha.core;

public final class Pose {
    public static final int E_OPEN = 0, E_WIDE = 1, E_BLINK = 2, E_HAPPY = 3, E_CLOSED = 4, E_SQUINT = 5, E_DIZZY = 6;
    public static final int M_SMILE = 0, M_GRIN = 1, M_OPEN = 2, M_O = 3, M_FLAT = 4, M_WAVY = 5;

    public float drop, bob, tilt, legL, legR, lenL, lenR, hRx, hRy, hLx, hLy, blush, fx, hair, wing, tailW;
    public int eyes, mouth;
    public String prop;

    public Pose reset(Skin sk) {
        drop = 0; bob = 0; tilt = 0; legL = -.06f; legR = .06f; lenL = 16; lenR = 16;
        handR(.16f, 17); handL(.16f, 17);
        eyes = E_OPEN; mouth = M_SMILE; blush = .45f; fx = 1.6f; hair = 0; prop = sk.prop; wing = 7; tailW = 0;
        return this;
    }

    public void handR(float a, float len) {
        hRx = 11.5f + len * (float) Math.sin(a);
        hRy = -41 + len * (float) Math.cos(a);
    }

    public void handL(float a, float len) {
        hLx = -(11.5f + len * (float) Math.sin(a));
        hLy = -41 + len * (float) Math.cos(a);
    }
}
