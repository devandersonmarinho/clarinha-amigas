package com.marinho.clarinha.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/** Simulação: comportamentos, física, toques, estrelinhas e partículas. */
public final class World {
    public interface Sound { void play(String k); }

    public static final class Part {
        public float x, y, vx, vy, life, max, s, r;
        public int type, col, owner;
    }
    public static final int P_SPARK = 0, P_STAR = 1, P_HEART = 2, P_Z = 3, P_NOTE = 4, P_RB = 5;

    static final class Ptr {
        Pet p; float x0, y0, downT; boolean moved, petMode;
        final float[][] s = new float[8][3]; int n;
        void add(float x, float y, float tm) {
            if (n == 8) { System.arraycopy(s, 1, s, 0, 7); n = 7; s[7] = new float[3]; }
            s[n][0] = x; s[n][1] = y; s[n][2] = tm; n++;
        }
    }

    public Sound sound;
    public float W = 800, H = 600, S = 1, CH = 100, G = 2200, floorY = 590, topY = 0, T = 0, dp = 1;
    public boolean allowStars = true, overlay = false;
    public final ArrayList<Pet> pets = new ArrayList<>();
    /** x, y, vy, rest, life */
    public final ArrayList<float[]> stars = new ArrayList<>();
    public final ArrayList<Part> parts = new ArrayList<>();
    /** Ícones: left, top, right, bottom (px). */
    public final ArrayList<float[]> plats = new ArrayList<>();
    public final Random rnd = new Random();
    private final HashMap<Integer, Ptr> ptrs = new HashMap<>();
    private final Pose pose = new Pose();
    private final float[] tmp = new float[2];
    private final float[] xf = new float[7];

    static final float PI = (float) Math.PI;
    static float sin(float a) { return (float) Math.sin(a); }
    static float cos(float a) { return (float) Math.cos(a); }
    static float abs(float a) { return Math.abs(a); }
    public static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    float rand(float a, float b) { return a + rnd.nextFloat() * (b - a); }
    boolean chance(float p) { return rnd.nextFloat() < p; }
    <T> T pick(T[] a) { return a[rnd.nextInt(a.length)]; }
    void sfx(String k) { if (sound != null) sound.play(k); }

    /* ------------------------------------------------------------------ configuração */
    public void setSize(float w, float h, float charHeightPx, float floor, float top, float density) {
        W = w; H = h; dp = density; CH = charHeightPx; S = CH / 100f; G = 22 * CH; floorY = floor; topY = top;
        for (Pet p : pets) {
            p.x = clamp(p.x, 22 * S, W - 22 * S);
            if ("climb".equals(p.state)) p.x = p.side == 'L' ? 29 * S : W - 29 * S;
            if (p.y > floorY) { p.y = floorY; p.plat = -1; setState(p, "idle", 1); }
        }
        for (float[] s : stars) { s[0] = clamp(s[0], 12, W - 12); if (s[1] > floorY) s[1] = floorY; }
    }

    public void setPlats(java.util.List<float[]> list) {
        plats.clear();
        if (list != null) plats.addAll(list);
        for (Pet p : pets) {
            String st = p.state;
            if (p.plat >= 0 || st.startsWith("ladder") || st.startsWith("rope") || "stepOn".equals(st)) {
                p.plat = -1; p.ladderA = 0; p.ropeA = 0; p.plan = null; p.vx = 0; p.vy = 0;
                if (!"held".equals(st)) setState(p, "air", 99);
            }
        }
    }

    public Pet addPet(int skin, float x) {
        Pet p = new Pet(skin);
        p.x = clamp(x, 30 * S, W - 30 * S);
        p.y = topY + 105 * S;
        p.facing = chance(.5f) ? 1 : -1;
        p.blinkT = rand(2.8f, 4.6f);
        setState(p, "air", 99);
        pets.add(p);
        return p;
    }

    public void removePet(Pet p) {
        float[] c = l2w(p, 0, -50);
        burst(c[0], c[1], P_SPARK, 20, 0);
        sfx("pop");
        pets.remove(p);
    }

    public void changeSkin(Pet p, int skin) {
        p.skin = skin;
        float[] c = l2w(p, 0, -50);
        burst(c[0], c[1], P_SPARK, 16, p.id);
        sfx("star");
        say(p, pick(Skin.ALL[skin].say), 1.5f);
    }

    /* ------------------------------------------------------------------ utilidades */
    void setState(Pet p, String st, float dur) { p.state = st; p.t = 0; p.dur = dur; p.var = chance(.5f) ? 1 : 0; }
    void say(Pet p, String txt, float dur) { p.bub = txt; p.bubT = dur; }
    static boolean onFloor(String st) {
        switch (st) {
            case "idle": case "walk": case "run": case "fetch": case "sit": case "sleep": case "wave": case "dance":
            case "look": case "hit": case "dizzy": case "bump": case "pet": case "happy": case "crouch": case "land":
            case "ropeTie": case "ladderPlace":
                return true;
            default:
                return false;
        }
    }
    public boolean onSurface(Pet p) { return onFloor(p.state) || "stepOn".equals(p.state); }
    float ground(Pet p) { return p.plat >= 0 && p.plat < plats.size() ? plats.get(p.plat)[1] : floorY; }

    /** tx, ty, rot, ox, oy, sx, sy */
    public float[] xform(Pet p) {
        float[] X = xf;
        if ("held".equals(p.state)) { X[0] = p.px; X[1] = p.py; X[2] = -p.phi; X[3] = 0; X[4] = 97 * S; X[5] = p.facing * S; X[6] = S; }
        else if ("sleep".equals(p.state)) { X[0] = p.x; X[1] = p.y - 24 * S; X[2] = -p.facing * PI / 2; X[3] = 0; X[4] = 0; X[5] = p.facing * S; X[6] = S; }
        else { X[0] = p.x; X[1] = p.y; X[2] = p.rot; X[3] = 0; X[4] = 0; X[5] = p.facing * S * (1 + p.sq * .7f); X[6] = S * (1 - p.sq); }
        return X;
    }
    public float[] l2w(Pet p, float lx, float ly) {
        float[] X = xform(p);
        float x = lx * X[5] + X[3], y = ly * X[6] + X[4], c = cos(X[2]), s = sin(X[2]);
        return new float[]{X[0] + x * c - y * s, X[1] + x * s + y * c};
    }

    float[] nearestStar(Pet p) {
        float[] b = null; float bd = 1e9f;
        for (float[] s : stars) { if (s[3] == 0) continue; float d = abs(s[0] - p.x); if (d < bd) { bd = d; b = s; } }
        return b;
    }

    /* ------------------------------------------------------------------ decisões */
    void pickNext(Pet p) {
        if (p.plat >= 0) { pickOnPlat(p); return; }
        Skin sk = Skin.ALL[p.skin];
        float[] st = nearestStar(p);
        if (st != null && chance(.8f)) { p.target = st[0]; setState(p, "fetch", 10); if (chance(.4f)) say(p, "Uma estrela!", 1.5f); return; }
        Pet friend = null;
        for (Pet q : pets) if (q != p && q.plat < 0 && ("idle".equals(q.state) || "walk".equals(q.state) || "sit".equals(q.state)) && abs(q.x - p.x) < 3.2f * CH) { friend = q; break; }
        String[] k = {"walk", "idle", "run", "sit", "wave", "jump", "dance", "look", "climb", "sleep", "fly", "greet", "goIcon"};
        float[] w = {30, 14, 6, 9, 6, 6, 6, 5, 10, p.awake > 45 ? 8 : 1, sk.fly ? 20 : 0, friend != null ? 9 : 0, upperPlats() > 0 ? 26 : 0};
        String c = choose(k, w);
        float M = 24 * S;
        switch (c) {
            case "walk": case "run": p.target = rand(M, W - M); setState(p, c, 12); break;
            case "climb": p.target = p.x < W / 2 ? 29 * S : W - 29 * S; p.wantClimb = true; setState(p, chance(.5f) ? "walk" : "run", 14); break;
            case "sit": setState(p, "sit", rand(3, 7)); break;
            case "wave": setState(p, "wave", 1.9f); say(p, pick(sk.say), 1.5f); break;
            case "jump": setState(p, "crouch", .2f); p.next = "jump"; break;
            case "dance": setState(p, "dance", rand(3, 5)); break;
            case "look": setState(p, "look", 2.6f); say(p, "?", 1); break;
            case "sleep":
                p.x = p.facing > 0 ? Math.max(p.x, 105 * S) : Math.min(p.x, W - 105 * S);
                setState(p, "sleep", rand(7, 14)); p.awake = 0; break;
            case "fly":
                setState(p, "fly", rand(4, 8)); p.fy = rand(topY + (H - topY) * .22f, topY + (H - topY) * .62f); p.target = rand(M, W - M);
                if (chance(.5f)) say(p, "Lá vou eu!", 1.5f);
                break;
            case "greet":
                p.facing = friend.x > p.x ? 1 : -1; setState(p, "wave", 1.9f); say(p, "Oi, amiga!", 1.5f);
                if (!"sit".equals(friend.state) || chance(.5f)) { friend.facing = -p.facing; setState(friend, "wave", 1.9f); say(friend, pick(new String[]{"Oii!", "Olá!", "Vamos brincar?"}), 1.6f); }
                break;
            case "goIcon": goIcon(p); break;
            default: setState(p, "idle", rand(1.5f, 4));
        }
    }

    int upperPlats() {
        int n = 0;
        for (float[] q : plats) if (q[1] < floorY - .4f * CH && q[1] > topY + 1.05f * CH) n++;
        return n;
    }

    String choose(String[] k, float[] w) {
        float tot = 0; for (float v : w) tot += v;
        float r = rnd.nextFloat() * tot;
        for (int i = 0; i < k.length; i++) { r -= w[i]; if (r < 0) return k[i]; }
        return "idle";
    }

    void goIcon(Pet p) {
        ArrayList<Integer> c = new ArrayList<>();
        for (int i = 0; i < plats.size(); i++) { float[] q = plats.get(i); if (q[1] < floorY - .4f * CH && q[1] > topY + 1.05f * CH) c.add(i); }
        if (c.isEmpty()) { setState(p, "idle", 1); return; }
        int idx = c.get(rnd.nextInt(c.size()));
        float[] q = plats.get(idx);
        float cx = (q[0] + q[2]) / 2, h = floorY - q[1];
        p.goal = idx;
        if (h < 1.25f * CH) {
            p.plan = "jump";
            p.target = clamp(p.x < cx ? cx - .9f * CH : cx + .9f * CH, 20 * S, W - 20 * S);
        } else {
            p.plan = "ladder";
            float lx = p.x < cx ? q[0] - .2f * CH : q[2] + .2f * CH;
            if (lx < 12 * S || lx > W - 12 * S) lx = p.x < cx ? q[2] + .2f * CH : q[0] - .2f * CH;
            p.target = clamp(lx, 12 * S, W - 12 * S);
            if (chance(.35f)) say(p, "Vou pegar a escada!", 1.6f);
        }
        setState(p, chance(.6f) ? "walk" : "run", 14);
    }

    void pickOnPlat(Pet p) {
        float[] q = plats.get(p.plat);
        Skin sk = Skin.ALL[p.skin];
        float m = 12 * S, cx = (q[0] + q[2]) / 2;
        int hop = neighbor(p.plat);
        String[] k = {"idle", "sit", "wave", "dance", "look", "walkP", "sleep", "rope", "jumpDown", "hop"};
        float[] w = {12, 16, 6, 5, 6, 10, p.awake > 45 ? 6 : 1, 13, 5, hop >= 0 ? 12 : 0};
        String c = choose(k, w);
        switch (c) {
            case "sit": setState(p, "sit", rand(3, 7)); break;
            case "wave": setState(p, "wave", 1.9f); say(p, pick(sk.say), 1.5f); break;
            case "dance": setState(p, "dance", rand(2.5f, 4)); break;
            case "look": setState(p, "look", 2.6f); say(p, pick(new String[]{"?", "Que alto!", "Uau!"}), 1.2f); break;
            case "walkP": p.target = q[2] - q[0] > 2 * m ? rand(q[0] + m, q[2] - m) : cx; setState(p, "walk", 6); break;
            case "sleep": setState(p, "sleep", rand(6, 12)); p.awake = 0; break;
            case "rope": {
                int side = chance(.5f) ? -1 : 1;
                if (q[0] < .3f * CH) side = 1; else if (q[2] > W - .3f * CH) side = -1;
                p.plan = "rope"; p.hdir = side; p.target = side < 0 ? q[0] + m : q[2] - m;
                if (q[2] - q[0] < 2 * m) p.target = cx;
                setState(p, "walk", 6);
                break;
            }
            case "jumpDown": {
                int side = p.x < W / 2 ? 1 : -1;
                if (chance(.3f)) side = -side;
                if ((side < 0 && q[0] < .5f * CH) || (side > 0 && q[2] > W - .5f * CH)) side = -side;
                p.facing = side; p.plat = -1; p.vx = side * .9f * CH; p.vy = -.9f * CH; p.joy = chance(.6f);
                setState(p, "air", 99); sfx("jump"); if (p.joy) say(p, "Uhuu!", 1.2f);
                break;
            }
            case "hop": {
                float[] r = plats.get(hop);
                p.goal = hop; p.facing = (r[0] + r[2]) / 2 > cx ? 1 : -1; p.next = "hop";
                setState(p, "crouch", .22f);
                break;
            }
            default: setState(p, "idle", rand(1.5f, 4));
        }
    }

    int neighbor(int idx) {
        float[] q = plats.get(idx);
        float qc = (q[0] + q[2]) / 2;
        ArrayList<Integer> c = new ArrayList<>();
        for (int i = 0; i < plats.size(); i++) {
            if (i == idx) continue;
            float[] r = plats.get(i);
            float d = abs((r[0] + r[2]) / 2 - qc);
            if (abs(r[1] - q[1]) < .6f * CH && d > .3f * CH && d < 3.4f * CH && r[1] > topY + 1.05f * CH) c.add(i);
        }
        return c.isEmpty() ? -1 : c.get(rnd.nextInt(c.size()));
    }

    void jumpTo(Pet p, float tx, float ty) {
        float up = p.y - ty, apex = Math.max(up, 0) + .35f * CH;
        float vy0 = -(float) Math.sqrt(2 * G * apex);
        float disc = vy0 * vy0 - 2 * G * up;
        float tl = (float) ((-vy0 + Math.sqrt(Math.max(0, disc))) / G);
        p.vy = vy0; p.vx = (tx - p.x) / Math.max(.2f, tl); p.joy = true; p.plat = -1;
        p.facing = p.vx >= 0 ? 1 : -1;
        setState(p, "air", 99); sfx("jump");
    }

    void startClimb(Pet p, char side, boolean fromAir) {
        p.side = side; p.facing = side == 'L' ? -1 : 1; p.x = side == 'L' ? 29 * S : W - 29 * S; p.climbDir = -1;
        p.vx = 0; p.vy = 0; p.rot = 0; p.plat = -1;
        setState(p, "climb", rand(2, 5)); p.pause = fromAir ? .5f : 0;
        if (chance(.3f)) say(p, "Vou escalar!", 1.4f);
    }

    void collectCheck(Pet p) {
        for (int i = stars.size() - 1; i >= 0; i--) {
            float[] s = stars.get(i);
            float[] c = l2w(p, 0, -40);
            if (abs(s[0] - c[0]) < 20 * S && abs(s[1] - c[1]) < 48 * S) {
                stars.remove(i);
                burst(s[0], s[1], P_SPARK, 10, p.id); sfx("star");
                if (!"fly".equals(p.state) && !"air".equals(p.state)) { setState(p, "happy", 1.2f); p.sq = .15f; }
                say(p, pick(new String[]{"Eba!", "Peguei!", "Que lindo!", "Uma estrelinha!"}), 1.5f);
                return;
            }
        }
    }

    /* ------------------------------------------------------------------ atualização */
    void update(Pet p, float dt) {
        Skin sk = Skin.ALL[p.skin];
        p.t += dt; p.awake += dt;
        p.sqv += (-260 * p.sq - 15 * p.sqv) * dt; p.sq += p.sqv * dt;
        if (p.bub != null) { p.bubT -= dt; if (p.bubT <= 0) p.bub = null; }
        String st = p.state;
        switch (st) {
            case "idle": case "sit": case "dance": case "wave": case "look": case "happy": case "land": case "hit":
                if ("look".equals(st) && p.t > p.lookN) { p.facing *= -1; p.lookN = p.t + rand(.5f, 1); }
                if ("dance".equals(st) && chance(dt * 3)) { float[] c = l2w(p, rand(-20, 20), -100); burst(c[0], c[1], P_NOTE, 1, p.id); }
                if (p.t > p.dur) {
                    if ("land".equals(st) && p.intro) { p.intro = false; p.facing = 1; setState(p, "wave", 2); say(p, pick(new String[]{"Oi!", "Cheguei!", "Olá!"}), 1.6f); }
                    else pickNext(p);
                }
                break;
            case "walk": case "run": case "fetch": {
                if ("fetch".equals(st)) { float[] s = nearestStar(p); if (s == null || p.plat >= 0) { setState(p, "idle", 1); break; } p.target = s[0]; }
                float sp = ("walk".equals(st) ? .8f : 2.1f) * CH, dx = p.target - p.x, step = sp * dt;
                p.facing = dx >= 0 ? 1 : -1;
                p.ph += dt * ("walk".equals(st) ? 7.5f : 14);
                p.rot += (("walk".equals(st) ? 0 : p.facing * .1f) - p.rot) * Math.min(1, dt * 10);
                if (abs(dx) <= step) {
                    p.x = p.target;
                    if (p.wantClimb) { p.wantClimb = false; startClimb(p, p.x < W / 2 ? 'L' : 'R', false); }
                    else if ("ladder".equals(p.plan) && p.goal >= 0 && p.goal < plats.size()) {
                        p.plan = null; float[] q = plats.get(p.goal);
                        p.ladderX = p.x; p.ladderTop = q[1] - 2 * S; p.ladderBot = floorY; p.ladderA = 1; p.ladderProg = 0;
                        setState(p, "ladderPlace", .5f); sfx("pop");
                    } else if ("jump".equals(p.plan) && p.goal >= 0 && p.goal < plats.size()) {
                        p.plan = null; float[] q = plats.get(p.goal);
                        p.facing = (q[0] + q[2]) / 2 > p.x ? 1 : -1; p.next = "jumpTo"; setState(p, "crouch", .22f);
                    } else if ("rope".equals(p.plan) && p.plat >= 0) {
                        p.plan = null; float[] q = plats.get(p.plat);
                        int side = p.hdir; p.facing = side;
                        p.ropeX = clamp(side < 0 ? q[0] - .1f * CH : q[2] + .1f * CH, 8 * S, W - 8 * S);
                        p.ropeTop = q[1]; p.ropeA = 1; p.ropeProg = 0;
                        setState(p, "ropeTie", .8f);
                        if (chance(.5f)) say(p, "Vou descer de corda!", 1.6f);
                    } else if (!"fetch".equals(st)) setState(p, "idle", rand(1, 2.5f));
                } else p.x += Math.signum(dx) * step;
                if (sk.rainbow && !"walk".equals(st) && chance(dt * 30)) {
                    float[] q = l2w(p, -6, -18);
                    Part pt = part(q[0], q[1], P_RB, p.id);
                    pt.vx = -p.facing * 20; pt.vy = rand(-15, 5); pt.life = pt.max = .8f; pt.col = Skin.RAINBOW[rnd.nextInt(6)]; pt.s = rand(2.5f, 4.5f) * S / 1.3f;
                }
                if (p.t > p.dur) { p.wantClimb = false; p.plan = null; setState(p, "idle", 1); }
                break;
            }
            case "crouch":
                if (p.t > p.dur) {
                    if ("jumpTo".equals(p.next) && p.goal >= 0 && p.goal < plats.size()) {
                        float[] q = plats.get(p.goal); jumpTo(p, (q[0] + q[2]) / 2, q[1]);
                    } else if ("hop".equals(p.next) && p.goal >= 0 && p.goal < plats.size()) {
                        float[] q = plats.get(p.goal); jumpTo(p, (q[0] + q[2]) / 2 + rand(-.15f, .15f) * (q[2] - q[0]), q[1]);
                        if (chance(.4f)) say(p, "Hop!", 1);
                    } else {
                        p.vy = -(float) Math.sqrt(2 * G * 1.15f * CH); p.vx = p.facing * rand(0, .7f) * CH;
                        if (p.plat >= 0) p.vx = 0;
                        p.joy = true; p.plat = -1; setState(p, "air", 99); sfx("jump");
                    }
                    p.next = null;
                }
                break;
            case "ladderPlace":
                p.ladderProg = Math.min(1, p.t / p.dur);
                if (p.t > p.dur) setState(p, "ladderUp", 99);
                break;
            case "ladderUp": {
                if (p.goal < 0 || p.goal >= plats.size()) { setState(p, "air", 99); break; }
                float[] q = plats.get(p.goal);
                p.y -= .65f * CH * dt; p.ph += dt * 8;
                if (p.y <= q[1]) {
                    p.y = q[1]; p.plat = p.goal; p.stepFrom = p.x;
                    float w = q[2] - q[0];
                    p.stepTo = p.ladderX < (q[0] + q[2]) / 2 ? q[0] + Math.min(.3f * w, 14 * S) : q[2] - Math.min(.3f * w, 14 * S);
                    p.facing = p.stepTo > p.x ? 1 : -1;
                    setState(p, "stepOn", .35f);
                }
                break;
            }
            case "stepOn": {
                float k = Math.min(1, p.t / p.dur);
                p.x = p.stepFrom + (p.stepTo - p.stepFrom) * k; p.ph += dt * 7.5f;
                if (p.plat >= 0 && p.plat < plats.size()) p.y = plats.get(p.plat)[1];
                if (p.t > p.dur) { setState(p, "idle", rand(1, 2)); if (chance(.5f)) say(p, pick(new String[]{"Cheguei!", "Que vista!", "Subi!"}), 1.4f); }
                break;
            }
            case "ropeTie":
                p.ropeProg = Math.min(1, p.t / p.dur);
                if (p.t > p.dur) {
                    p.plat = -1; p.x = p.ropeX; p.y = p.ropeTop + 104 * S; p.facing = 1; setState(p, "rope", 99); sfx("whoosh");
                }
                break;
            case "rope": {
                float prev = p.y;
                p.y += .9f * CH * dt; p.ph += dt * 3; p.x = p.ropeX + sin(p.ph) * 2 * S;
                int land = landing(p, prev);
                if (land >= 0) { p.x = clamp(p.x, plats.get(land)[0], plats.get(land)[2]); landOn(p, land, .5f * CH); break; }
                if (p.y >= floorY) { p.y = floorY; p.x = p.ropeX; landOn(p, -1, .5f * CH); }
                break;
            }
            case "air": {
                float g = G * (sk.fly ? .26f : 1);
                p.vy += g * dt; if (sk.fly) p.vy = Math.min(p.vy, 1.3f * CH);
                float prev = p.y;
                p.x += p.vx * dt; p.y += p.vy * dt; p.rot *= (float) Math.exp(-3.5f * dt);
                if (sk.fly && chance(dt * 12)) { float[] c = l2w(p, 0, -40); burst(c[0], c[1], P_SPARK, 1, p.id); }
                float m = 20 * S;
                if (p.x < m || p.x > W - m) {
                    char side = p.x < m ? 'L' : 'R'; p.x = clamp(p.x, m, W - m);
                    if (abs(p.vx) > 2.4f * CH) { p.vx = -p.vx * .4f; p.sq = .2f; sfx("bump"); say(p, "Ai!", 1); burst(side == 'L' ? 0 : W, p.y - 50 * S, P_STAR, 4, p.id); }
                    else if (p.y < floorY - 1.3f * CH && !sk.fly && chance(.75f)) { startClimb(p, side, true); sfx("land"); break; }
                    else p.vx = -p.vx * .3f;
                }
                if (p.y - 97 * S < topY) {
                    p.y = topY + 97 * S;
                    if (abs(p.vy) < 2.2f * CH && !sk.fly && chance(.6f)) {
                        p.y = topY + 101 * S; p.hdir = p.vx >= 0 ? 1 : -1; p.facing = p.hdir; p.vx = 0; p.vy = 0; p.rot = 0;
                        setState(p, "hang", rand(2, 5)); say(p, "Peguei no teto!", 1.2f); break;
                    }
                    p.vy = abs(p.vy) * .35f;
                }
                collectCheck(p);
                if (p.vy > 0) {
                    int land = landing(p, prev);
                    if (land >= 0) { float v = p.vy; landOn(p, land, v); break; }
                }
                if (p.y >= floorY) { float v = p.vy; landOn(p, -1, v); }
                break;
            }
            case "climb": {
                if (p.pause > 0) p.pause -= dt; else { p.y += p.climbDir * .55f * CH * dt; p.ph += dt * 7; }
                if (p.pause <= 0 && chance(dt * .25f)) p.pause = rand(.5f, 1.4f);
                if (p.y - 101 * S <= topY) { p.y = topY + 101 * S; p.hdir = p.side == 'L' ? 1 : -1; p.facing = p.hdir; setState(p, "hang", rand(3, 7)); break; }
                if (p.y >= floorY && p.climbDir > 0) { p.y = floorY; p.facing *= -1; setState(p, "idle", 1); break; }
                if (p.t > p.dur) {
                    float r = rnd.nextFloat();
                    if (r < .4f) { p.vx = -p.facing * 1.5f * CH; p.vy = -.7f * CH; p.joy = chance(.6f); p.facing *= -1; setState(p, "air", 99); sfx("jump"); if (p.joy) say(p, "Uhuu!", 1.2f); }
                    else if (r < .6f) { p.climbDir = 1; p.dur = 99; }
                    else { p.dur = p.t + rand(2, 4); p.climbDir = -1; }
                }
                break;
            }
            case "hang":
                p.x += p.hdir * .5f * CH * dt; p.ph += dt * 5; p.facing = p.hdir; p.rot = sin(p.ph * .5f) * .06f;
                if (p.x > W - 29 * S || p.x < 29 * S) { char side = p.x < W / 2 ? 'L' : 'R'; p.y = topY + 101 * S; startClimb(p, side, false); p.climbDir = 1; p.dur = 99; break; }
                if (p.t > p.dur) { p.vx = 0; p.vy = 0; p.joy = false; setState(p, "air", 99); if (chance(.6f)) say(p, pick(new String[]{"Uiii!", "Lá vou eu!"}), 1.3f); }
                break;
            case "fly": {
                float dx = p.target - p.x, sp = 1.1f * CH;
                p.x += clamp(dx, -sp * dt, sp * dt); p.facing = dx >= 0 ? 1 : -1;
                p.y += (p.fy + sin(T * 2.2f) * 7 * S - p.y) * Math.min(1, dt * 1.6f); p.rot = p.facing * .08f;
                if (abs(dx) < 6) p.target = rand(24 * S, W - 24 * S);
                if (chance(dt * 10)) { float[] c = l2w(p, -4, -30); burst(c[0], c[1], P_SPARK, 1, p.id); }
                collectCheck(p);
                if (p.t > p.dur) { p.vx = 0; p.vy = 0; setState(p, "air", 99); }
                break;
            }
            case "held": {
                float vx = (p.px - p.lpx) / dt; p.lpx = p.px;
                p.svx += (vx - p.svx) * Math.min(1, dt * 18);
                float ax = (p.svx - p.lsvx) / dt; p.lsvx = p.svx;
                float L = .97f * CH;
                p.phw += (-(G / L) * sin(p.phi) - (ax / L) * cos(p.phi) * .7f - 3 * p.phw) * dt;
                p.phi = clamp(p.phi + p.phw * dt, -1.45f, 1.45f);
                p.hair = clamp(-p.phw * 1.5f, -4, 4);
                break;
            }
            case "pet":
                if (chance(dt * 4)) { float[] c = l2w(p, rand(-20, 20), -100); burst(c[0], c[1], P_HEART, 1, p.id); }
                if (chance(dt * .8f)) sfx("heart");
                break;
            case "dizzy": case "bump":
                if (p.t > p.dur) { setState(p, "idle", 1); if (chance(.5f)) say(p, pick(new String[]{"Já passou!", "Tô bem!"}), 1.4f); }
                break;
            case "sleep":
                if (chance(dt * .9f)) { float[] c = l2w(p, 0, -85); burst(c[0], c[1], P_Z, 1, p.id); }
                if (p.t > p.dur) { setState(p, "idle", 1.5f); say(p, pick(new String[]{"Que soninho bom!", "Acordei!"}), 1.6f); }
                break;
            default:
                break;
        }
        if (!"ladderPlace".equals(p.state) && !"ladderUp".equals(p.state) && !"stepOn".equals(p.state) && p.ladderA > 0) p.ladderA = Math.max(0, p.ladderA - dt * 2);
        if (!"ropeTie".equals(p.state) && !"rope".equals(p.state) && p.ropeA > 0) p.ropeA = Math.max(0, p.ropeA - dt * 1.5f);
        if (onFloor(p.state)) {
            if (p.plat >= plats.size()) p.plat = -1;
            p.y = ground(p);
            if (!"walk".equals(p.state) && !"run".equals(p.state) && !"fetch".equals(p.state)) p.rot *= (float) Math.exp(-8 * dt);
            if (p.plat < 0) collectCheck(p);
        }
    }

    /** Índice do ícone em que os pés cruzaram o topo neste passo, ou -1. */
    int landing(Pet p, float prevY) {
        for (int i = 0; i < plats.size(); i++) {
            float[] q = plats.get(i);
            if (prevY <= q[1] + 1 && p.y >= q[1] && p.x >= q[0] - 4 * S && p.x <= q[2] + 4 * S && q[1] > topY + 1.05f * CH) return i;
        }
        return -1;
    }

    void landOn(Pet p, int plat, float v) {
        Skin sk = Skin.ALL[p.skin];
        p.plat = plat; p.y = plat >= 0 ? plats.get(plat)[1] : floorY;
        p.vx = 0; p.vy = 0; p.rot = 0;
        if (v > (float) Math.sqrt(2 * G * 2.4f * CH) && !sk.fly && !p.intro) {
            setState(p, "bump", 2.4f); p.sq = .35f; sfx("bump");
            say(p, pick(new String[]{"Ai, ai...", "Caí!", "Ufa!"}), 1.5f);
            burst(p.x, p.y - 80 * S, P_STAR, 5, p.id);
        } else {
            setState(p, "land", .4f); p.sq = Math.min(.32f, v / (CH * 18)); sfx("land");
            if (plat >= 0 && chance(.35f)) say(p, pick(new String[]{"Aterrissei!", "Opa!", "Cheguei!"}), 1.3f);
        }
        p.joy = false;
    }

    /* ------------------------------------------------------------------ pose */
    public Pose pose(Pet p) {
        Skin sk = Skin.ALL[p.skin];
        Pose P = pose.reset(sk);
        float t = T, ph = p.ph;
        boolean blink = ((t + p.blinkT * 3.1f) % p.blinkT) < .13f;
        if (blink) P.eyes = Pose.E_BLINK;
        P.hair = p.hair;
        switch (p.state) {
            case "idle": P.bob = sin(t * 2.2f) * .5f; if (p.var == 1) P.tilt = sin(t * 1.3f) * .07f; break;
            case "walk": case "run": case "fetch": case "stepOn": {
                float k = "run".equals(p.state) || "fetch".equals(p.state) ? 1.5f : 1, sn = sin(ph);
                P.legR = sn * .5f * k; P.legL = -sn * .5f * k; P.bob = -abs(cos(ph)) * 1.6f * k;
                P.handR(.2f + sn * .35f * k, 17); P.handL(.2f - sn * .35f * k, 17);
                if (k > 1) { P.mouth = Pose.M_GRIN; P.tailW = .3f; }
                break;
            }
            case "sit": case "ropeTie":
                P.drop = 15; P.legL = -1.35f + sin(t * 4) * .12f; P.legR = 1.35f - sin(t * 4 + 1) * .12f; P.lenL = P.lenR = 12;
                P.handR(.55f, 14); P.handL(.55f, 14);
                if (p.var == 1 && !blink) P.eyes = Pose.E_HAPPY;
                if ("ropeTie".equals(p.state)) { P.hRx = 16; P.hRy = -30 + sin(t * 12) * 2; P.prop = null; }
                break;
            case "sleep":
                P.eyes = Pose.E_CLOSED; P.mouth = Pose.M_O; P.bob = sin(t * 1.5f) * .6f; P.handR(.9f, 15); P.handL(.3f, 15); P.prop = null; P.blush = .6f;
                break;
            case "wave":
                P.fx = .4f; P.hRx = 30 + sin(t * 14) * 3; P.hRy = -62 + cos(t * 14) * 1.5f; P.mouth = Pose.M_GRIN;
                if (!blink && p.t > .8f) P.eyes = Pose.E_HAPPY;
                P.prop = null;
                break;
            case "dance": {
                float s6 = sin(t * 7);
                P.bob = -abs(s6) * 3.5f; P.handR(2.3f + s6 * .4f, 25); P.handL(2.3f - s6 * .4f, 25);
                P.legR = .15f + Math.max(0, s6) * .5f; P.legL = -.15f - Math.max(0, -s6) * .5f; P.mouth = Pose.M_GRIN;
                if (!blink) P.eyes = Pose.E_HAPPY;
                P.tilt = s6 * .1f; P.prop = null; P.tailW = s6 * .4f;
                break;
            }
            case "look": P.tilt = sin(p.t * 2.5f) * .12f; P.mouth = Pose.M_O; break;
            case "hit":
                P.eyes = Pose.E_SQUINT; P.mouth = Pose.M_OPEN; P.tilt = -.28f * Math.max(0, 1 - p.t * 2.2f);
                P.handR(1.75f, 18); P.handL(1.75f, 18); P.prop = null; P.fx = 0;
                break;
            case "dizzy": case "bump":
                P.drop = 15; P.legL = -1.35f; P.legR = 1.35f; P.lenL = P.lenR = 12; P.eyes = Pose.E_DIZZY; P.mouth = Pose.M_WAVY;
                P.tilt = sin(t * 5) * .14f; P.handR(.6f, 14); P.handL(.6f, 14); P.prop = null; P.fx = 0;
                break;
            case "pet":
                P.eyes = Pose.E_HAPPY; P.blush = .95f; P.mouth = Pose.M_GRIN; P.tilt = sin(t * 3) * .12f;
                P.hRx = 6; P.hRy = -34; P.hLx = -6; P.hLy = -34; P.fx = 0; P.prop = null;
                break;
            case "happy":
                P.eyes = Pose.E_HAPPY; P.mouth = Pose.M_GRIN; P.handR(2.6f, 25); P.handL(2.6f, 25); P.blush = .8f; P.fx = 0;
                P.bob = -abs(sin(p.t * 9)) * 4;
                break;
            case "crouch": P.drop = 6; P.legL = -.5f; P.legR = .5f; P.lenL = P.lenR = 13; P.handR(.6f, 15); P.handL(.6f, 15); break;
            case "land": P.legL = -.28f; P.legR = .28f; P.handR(.7f, 17); P.handL(.7f, 17); break;
            case "ladderPlace":
                P.handR(2.2f, 22); P.handL(2.2f, 22); P.mouth = Pose.M_GRIN; P.prop = null; P.fx = 0;
                break;
            case "climb": {
                float sn = sin(ph);
                P.legR = .75f + sn * .35f; P.legL = .25f - sn * .35f; P.lenL = P.lenR = 14;
                P.hRx = 30; P.hRy = -76 + sn * 4; P.handL(.45f, 16); P.prop = null; P.tilt = .06f;
                break;
            }
            case "ladderUp": {
                float sn = sin(ph);
                P.hRx = 9; P.hRy = -98 - Math.max(0, sn) * 5; P.hLx = -9; P.hLy = -98 - Math.max(0, -sn) * 5;
                P.legR = .12f + Math.max(0, sn) * .5f; P.lenR = 16 - Math.max(0, sn) * 5;
                P.legL = -.12f - Math.max(0, -sn) * .5f; P.lenL = 16 - Math.max(0, -sn) * 5;
                P.prop = null; P.fx = 0; P.mouth = Pose.M_GRIN;
                break;
            }
            case "rope":
                P.hRx = 3; P.hRy = -103; P.hLx = -2; P.hLy = -97;
                P.legR = .06f + sin(t * 3) * .1f; P.legL = -.02f + sin(t * 3) * .1f; P.lenL = P.lenR = 15;
                P.prop = null; P.fx = 0; P.mouth = p.var == 1 ? Pose.M_GRIN : Pose.M_O;
                if (!blink) P.eyes = p.var == 1 ? Pose.E_HAPPY : Pose.E_OPEN;
                break;
            case "hang": {
                float sn = sin(ph);
                P.hRx = 15; P.hRy = -101 - Math.max(0, sn) * 4; P.hLx = -15; P.hLy = -101 - Math.max(0, -sn) * 4;
                P.legR = .12f + sin(t * 2.2f) * .18f; P.legL = -.12f + sin(t * 2.2f + .8f) * .18f; P.prop = null; P.fx = .8f;
                break;
            }
            case "held": {
                float w = sin(t * 18), k = sin(t * 15);
                P.hRx = 31 + w * 3; P.hRy = -56 + cos(t * 18) * 4;
                P.hLx = -31 - sin(t * 18 + 1.3f) * 3; P.hLy = -56 + cos(t * 18 + 1.3f) * 4;
                P.legR = .25f + k * .45f; P.legL = -.25f + k * .45f;
                P.mouth = p.var == 1 ? Pose.M_GRIN : Pose.M_OPEN;
                if (!blink) P.eyes = p.var == 1 ? Pose.E_HAPPY : Pose.E_WIDE;
                P.fx = 0; P.prop = null; P.wing = 26; P.tailW = -p.phi;
                break;
            }
            case "air":
                if (p.joy) { P.handR(2.6f, 25); P.handL(2.6f, 25); P.eyes = Pose.E_HAPPY; P.mouth = Pose.M_GRIN; P.legL = -.35f; P.legR = .35f; P.fx = 0; }
                else {
                    float w = sin(t * 16);
                    P.hRx = 28; P.hRy = -60 + w * 3; P.hLx = -28; P.hLy = -60 - w * 3; P.legR = .35f; P.legL = -.35f;
                    P.mouth = sk.fly ? Pose.M_SMILE : Pose.M_O;
                    if (!blink) P.eyes = sk.fly ? Pose.E_OPEN : Pose.E_WIDE;
                    P.fx = 0;
                }
                P.prop = sk.fly ? sk.prop : null; P.wing = 26;
                break;
            case "fly": {
                float s = sin(t * 3);
                P.hRx = 24; P.hRy = -44 + s * 2; P.hLx = -24; P.hLy = -44 - s * 2; P.legR = .28f + s * .08f; P.legL = .06f + s * .08f; P.wing = 26;
                break;
            }
            default: break;
        }
        return P;
    }

    /* ------------------------------------------------------------------ partículas */
    Part part(float x, float y, int type, int owner) {
        Part q = new Part(); q.x = x; q.y = y; q.type = type; q.owner = owner; q.life = q.max = 1; q.s = 1; q.col = 0xFFFFE066;
        parts.add(q);
        return q;
    }
    public void burst(float x, float y, int type, int n, int owner) {
        int[] sc = {0xFFFFFFFF, 0xFFFFE066, 0xFF9FF3FF, 0xFFFFC6EC};
        int[] nc = {0xFFFFE066, 0xFF9FF3FF, 0xFFFFC6EC};
        for (int i = 0; i < n; i++) {
            float a = rand(0, 2 * PI), v = type == P_SPARK ? rand(30, 120) : rand(20, 70);
            boolean up = type == P_Z || type == P_HEART || type == P_NOTE;
            float k = S * 1.3f;
            Part q = part(x, y, type, owner);
            q.vx = cos(a) * v * (up ? .3f : 1) * k;
            q.vy = (up ? -rand(25, 45) : sin(a) * v - 30) * k;
            q.life = q.max = type == P_Z ? 1.8f : 1;
            q.col = type == P_SPARK ? sc[rnd.nextInt(4)] : type == P_NOTE ? nc[rnd.nextInt(3)] : 0xFFFFE066;
            q.s = rand(.8f, 1.3f); q.r = rand(0, 2 * PI);
        }
        if (parts.size() > 400) parts.subList(0, parts.size() - 400).clear();
    }

    /* ------------------------------------------------------------------ estrelinhas */
    public void spawnStar(float x, float y) {
        if (stars.size() >= 6) stars.remove(0);
        stars.add(new float[]{clamp(x, 12, W - 12), y, 0, 0, 30});
        sfx("pop"); burst(x, y, P_SPARK, 5, 0);
    }

    /* ------------------------------------------------------------------ toque */
    public Pet hitTest(float x, float y) {
        for (int i = pets.size() - 1; i >= 0; i--) {
            Pet p = pets.get(i);
            if ("held".equals(p.state)) continue;
            float[] c = l2w(p, 0, -52);
            if (Math.hypot(x - c[0], y - c[1]) < 50 * S) return p;
        }
        return null;
    }

    public Pet pointerDown(int id, float x, float y, float tm) {
        Pet p = hitTest(x, y);
        Ptr r = new Ptr(); r.p = p; r.x0 = x; r.y0 = y; r.downT = tm; r.add(x, y, tm);
        ptrs.put(id, r);
        return p;
    }

    public void pointerMove(int id, float x, float y, float tm) {
        Ptr r = ptrs.get(id);
        if (r == null) return;
        r.add(x, y, tm);
        if (!r.moved && Math.hypot(x - r.x0, y - r.y0) > 12 * dp) {
            r.moved = true;
            if (r.p != null && !"held".equals(r.p.state) && pets.contains(r.p)) grab(r.p, x, y);
        }
        if (r.p != null && "held".equals(r.p.state)) { r.p.px = x; r.p.py = y; }
    }

    public void pointerUp(int id, float x, float y, float tm, boolean cancel) {
        Ptr r = ptrs.remove(id);
        if (r == null) return;
        Pet p = r.p;
        if (p != null && pets.contains(p)) {
            if ("held".equals(p.state)) {
                float vx = 0, vy = 0;
                int a = -1;
                for (int i = 0; i < r.n; i++) if (tm - r.s[i][2] < .1f) { a = i; break; }
                if (a >= 0 && a < r.n - 1) {
                    float[] A = r.s[a], B = r.s[r.n - 1];
                    float dt = Math.max(.016f, B[2] - A[2]);
                    vx = (B[0] - A[0]) / dt; vy = (B[1] - A[1]) / dt;
                }
                release(p, vx, vy);
            } else if (r.petMode) setState(p, "happy", 1);
            else if (!r.moved && !cancel) {
                if (T - p.lastTap < .33f && onFloor(p.state)) { setState(p, "crouch", .15f); p.next = "jump"; say(p, "Iupiii!", 1.2f); }
                else hitPet(p);
                p.lastTap = T;
            }
        } else if (p == null && !r.moved && !cancel && allowStars) spawnStar(r.x0, r.y0);
    }

    void grab(Pet p, float x, float y) {
        setState(p, "held", 99); p.var = chance(.35f) ? 1 : 0;
        p.px = x; p.py = y; p.lpx = x; p.svx = 0; p.lsvx = 0; p.phi = 0; p.phw = 0; p.wantClimb = false; p.plan = null; p.plat = -1; p.next = null;
        say(p, pick(new String[]{"Uiii!", "Me põe no chão!", "Tô voando!", "Hihi, cócegas!"}), 1.5f);
        sfx("grab");
    }

    void release(Pet p, float vx, float vy) {
        float L = 97 * S;
        p.x = p.px + L * sin(p.phi); p.y = Math.min(floorY, p.py + L * cos(p.phi)); p.rot = -p.phi;
        float sp = (float) Math.hypot(vx, vy), mx = 30 * CH;
        if (sp > mx) { vx *= mx / sp; vy *= mx / sp; }
        p.vx = vx; p.vy = vy; p.joy = false; p.hair = 0; p.plat = -1;
        setState(p, "air", 99);
        if (sp > 3.5f * CH) { sfx("whoosh"); say(p, pick(new String[]{"Uhuuuu!", "Iupiii!", "Aaaah!"}), 1.4f); }
    }

    void hitPet(Pet p) {
        for (int i = p.hits.size() - 1; i >= 0; i--) if (T - p.hits.get(i) >= 3) p.hits.remove(i);
        p.hits.add(T);
        sfx("hit");
        float[] h = l2w(p, 0, -75);
        burst(h[0], h[1], P_STAR, 3, p.id);
        String st = p.state;
        if ("sleep".equals(st)) { setState(p, "hit", .6f); say(p, "Hã?!", 1.2f); return; }
        boolean hanging = "climb".equals(st) || "hang".equals(st) || "fly".equals(st) || "rope".equals(st) || "ladderUp".equals(st) || "stepOn".equals(st);
        if (hanging || (p.plat >= 0 && onFloor(st))) {
            p.plat = -1; p.vx = rand(-.6f, .6f) * CH; p.vy = -.4f * CH; p.joy = false; p.plan = null;
            setState(p, "air", 99); say(p, "Uaaa!", 1.2f); return;
        }
        if ("air".equals(st)) { p.vx += rand(-1, 1) * 1.5f * CH; p.vy = -1.2f * CH; say(p, "Ui!", 1); return; }
        if (p.hits.size() >= 4) { p.hits.clear(); setState(p, "dizzy", 2.4f); say(p, "Tô tontinha...", 1.6f); return; }
        setState(p, "hit", .55f); p.sq = .22f; p.x = clamp(p.x - p.facing * 7 * S, 22 * S, W - 22 * S);
        say(p, pick(new String[]{"Ai!", "Ui!", "Ei!", "Ops!", "Hihi!"}), 1);
    }

    /* ------------------------------------------------------------------ passo */
    public void step(float dt) {
        if (dt <= 0) return;
        dt = Math.min(dt, .033f);
        T += dt;
        for (int i = 0; i < pets.size(); i++) update(pets.get(i), dt);
        for (int i = stars.size() - 1; i >= 0; i--) {
            float[] s = stars.get(i);
            s[4] -= dt;
            if (s[4] <= 0) { stars.remove(i); continue; }
            if (s[3] == 0) {
                s[2] += G * .35f * dt; s[1] += s[2] * dt;
                if (s[1] >= floorY - 6 * S) { s[1] = floorY - 6 * S; if (s[2] > 200) s[2] = -s[2] * .35f; else { s[2] = 0; s[3] = 1; } }
            }
        }
        for (int i = parts.size() - 1; i >= 0; i--) {
            Part q = parts.get(i);
            q.life -= dt;
            if (q.life <= 0) { parts.remove(i); continue; }
            q.x += q.vx * dt; q.y += q.vy * dt;
            if (q.type == P_SPARK || q.type == P_STAR) q.vy += 120 * S * dt;
            q.r += dt * 3;
        }
    }

    /** Verifica a pressão longa (carinho). tm = relógio dos toques em segundos. */
    public void checkHold(float tm) {
        for (Ptr r : ptrs.values()) {
            Pet p = r.p;
            if (p == null || r.moved || r.petMode || !pets.contains(p)) continue;
            if (tm - r.downT > .42f && !"held".equals(p.state) && !"air".equals(p.state)) {
                r.petMode = true; setState(p, "pet", 99);
                say(p, pick(new String[]{"Que gostoso!", "Hihi!", "Mais carinho!"}), 1.5f);
            }
        }
    }

    public boolean hasPointers() { return !ptrs.isEmpty(); }
}
