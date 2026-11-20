package com.marinho.clarinha.core;

import java.util.ArrayList;

public final class Pet {
    private static int seq = 1;
    public final int id = seq++;
    public int skin;
    public float x, y, vx, vy, t, dur, ph, rot, sq, sqv, blinkT, awake, phi, phw, px, py, lpx, svx, lsvx, pause, target, lookN, fy, hair;
    public int facing = 1, var, climbDir = -1, hdir = 1;
    public String state = "air", next, plan;
    public final ArrayList<Float> hits = new ArrayList<>();
    public String bub;
    public float bubT, lastTap = -9;
    public boolean joy, intro = true, wantClimb;
    public char side = 'L';
    /** Plataforma (ícone) onde está apoiada; -1 = chão. */
    public int plat = -1, goal = -1;
    public float ladderX, ladderTop, ladderBot, ladderA, ladderProg;
    public float ropeX, ropeTop, ropeA, ropeProg;
    public float stepFrom, stepTo;

    public Pet(int skin) { this.skin = skin; }
}
