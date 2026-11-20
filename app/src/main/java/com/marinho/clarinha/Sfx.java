package com.marinho.clarinha;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.marinho.clarinha.core.World;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

/** Sons curtinhos sintetizados na hora (sem arquivos de áudio). */
public final class Sfx implements World.Sound {
    private static final int RATE = 22050;
    private final SoundPool pool;
    private final HashMap<String, Integer> ids = new HashMap<>();
    public boolean on = true;
    private long lastPlay;

    public Sfx(Context ctx) {
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        String[] names = {"hit", "grab", "land", "bump", "jump", "star", "pop", "heart", "whoosh"};
        for (String n : names) {
            try {
                File f = new File(ctx.getCacheDir(), "sfx_" + n + ".wav");
                if (!f.exists()) write(f, synth(n));
                ids.put(n, pool.load(f.getAbsolutePath(), 1));
            } catch (Exception ignored) { }
        }
    }

    @Override public void play(String k) {
        if (!on) return;
        Integer id = ids.get(k);
        if (id == null) return;
        long now = System.currentTimeMillis();
        if ("heart".equals(k) && now - lastPlay < 120) return;
        lastPlay = now;
        pool.play(id, .8f, .8f, 1, 0, 1);
    }

    public void release() { pool.release(); }

    private static float[] synth(String k) {
        float[] b = new float[(int) (RATE * .5f)];
        switch (k) {
            case "hit": tone(b, 720, 260, .13f, 1, .5f, 0); break;
            case "grab": tone(b, 420, 980, .24f, 0, .4f, 0); break;
            case "land": tone(b, 230, 95, .12f, 0, .5f, 0); break;
            case "bump": tone(b, 170, 60, .28f, 1, .6f, 0); tone(b, 900, 500, .12f, 0, .2f, .08f); break;
            case "jump": tone(b, 330, 780, .16f, 0, .4f, 0); break;
            case "star": tone(b, 988, 990, .12f, 0, .35f, 0); tone(b, 1319, 1320, .22f, 0, .35f, .09f); break;
            case "pop": tone(b, 600, 1250, .07f, 0, .3f, 0); break;
            case "heart": tone(b, 660, 900, .15f, 0, .22f, 0); break;
            case "whoosh": tone(b, 1000, 220, .28f, 2, .12f, 0); break;
            default: break;
        }
        return b;
    }

    /** type: 0 senoide, 1 triangular, 2 dente de serra */
    private static void tone(float[] b, float f0, float f1, float dur, int type, float vol, float delay) {
        int s0 = (int) (delay * RATE), n = (int) (dur * RATE);
        double ph = 0;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float k = i / (float) n;
            double f = f0 * Math.pow(f1 / f0, k);
            ph += 2 * Math.PI * f / RATE;
            double x = ph / (2 * Math.PI) % 1.0;
            double v;
            if (type == 1) v = 4 * Math.abs(x - .5) - 1; else if (type == 2) v = 2 * x - 1; else v = Math.sin(ph);
            double env = vol * Math.pow(.001 / 1.0, k) * Math.min(1, i / (RATE * .004));
            b[s0 + i] += (float) (v * env);
        }
    }

    private static void write(File f, float[] b) throws Exception {
        int n = b.length;
        byte[] out = new byte[44 + n * 2];
        put(out, 0, "RIFF"); int32(out, 4, 36 + n * 2); put(out, 8, "WAVE"); put(out, 12, "fmt ");
        int32(out, 16, 16); int16(out, 20, 1); int16(out, 22, 1); int32(out, 24, RATE); int32(out, 28, RATE * 2);
        int16(out, 32, 2); int16(out, 34, 16); put(out, 36, "data"); int32(out, 40, n * 2);
        for (int i = 0; i < n; i++) {
            int v = (int) Math.max(-32767, Math.min(32767, b[i] * 32767));
            int16(out, 44 + i * 2, v);
        }
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(out); }
    }
    private static void put(byte[] o, int at, String s) { for (int i = 0; i < 4; i++) o[at + i] = (byte) s.charAt(i); }
    private static void int32(byte[] o, int at, int v) { o[at] = (byte) v; o[at + 1] = (byte) (v >> 8); o[at + 2] = (byte) (v >> 16); o[at + 3] = (byte) (v >> 24); }
    private static void int16(byte[] o, int at, int v) { o[at] = (byte) v; o[at + 1] = (byte) (v >> 8); }
}
