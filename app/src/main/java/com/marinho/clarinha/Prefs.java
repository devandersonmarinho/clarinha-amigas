package com.marinho.clarinha;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/** Preferências salvas no aparelho. */
public final class Prefs {
    private Prefs() {}

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("clarinha", Context.MODE_PRIVATE); }

    public static int[] team(Context c) {
        String s = sp(c).getString("team", "0");
        ArrayList<Integer> l = new ArrayList<>();
        for (String p : s.split(",")) {
            try { int v = Integer.parseInt(p.trim()); if (v >= 0 && v < 8) l.add(v); } catch (Exception ignored) { }
        }
        if (l.isEmpty()) l.add(0);
        int[] r = new int[Math.min(5, l.size())];
        for (int i = 0; i < r.length; i++) r[i] = l.get(i);
        return r;
    }

    public static void setTeam(Context c, List<Integer> skins) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < skins.size(); i++) { if (i > 0) b.append(','); b.append(skins.get(i)); }
        sp(c).edit().putString("team", b.toString()).apply();
    }

    public static String scene(Context c) { return sp(c).getString("scene", "noite"); }
    public static void setScene(Context c, String s) { sp(c).edit().putString("scene", s).apply(); }
    public static boolean sound(Context c) { return sp(c).getBoolean("sound", true); }
    public static void setSound(Context c, boolean v) { sp(c).edit().putBoolean("sound", v).apply(); }
    public static boolean homeOn(Context c) { return sp(c).getBoolean("homeOn", false); }
    public static void setHomeOn(Context c, boolean v) { sp(c).edit().putBoolean("homeOn", v).apply(); }
    public static int homeCount(Context c) { return sp(c).getInt("homeCount", 1); }
    public static void setHomeCount(Context c, int v) { sp(c).edit().putInt("homeCount", v).apply(); }

    public static boolean tablet(Context c) { return c.getResources().getConfiguration().smallestScreenWidthDp >= 600; }

    public static float iconDp(Context c) { return sp(c).getFloat("iconDp", tablet(c) ? 64 : 56); }
    public static void setIconDp(Context c, float v) { sp(c).edit().putFloat("iconDp", v).apply(); }

    static String key(Context c) {
        return c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? "iconsL" : "iconsP";
    }

    /** Centros dos ícones em frações da tela (x/W, y/H). */
    public static List<float[]> icons(Context c) {
        ArrayList<float[]> l = new ArrayList<>();
        String s = sp(c).getString(key(c), "");
        if (s.isEmpty()) return l;
        for (String p : s.split(";")) {
            String[] xy = p.split(",");
            if (xy.length != 2) continue;
            try { l.add(new float[]{Float.parseFloat(xy[0]), Float.parseFloat(xy[1])}); } catch (Exception ignored) { }
        }
        return l;
    }

    public static void setIcons(Context c, List<float[]> l) {
        StringBuilder b = new StringBuilder();
        for (float[] q : l) { if (b.length() > 0) b.append(';'); b.append(q[0]).append(',').append(q[1]); }
        sp(c).edit().putString(key(c), b.toString()).apply();
    }

    public static boolean canOverlay(Context c) { return Settings.canDrawOverlays(c); }

    @SuppressWarnings("deprecation")
    public static boolean usageGranted(Context c) {
        AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode;
        if (Build.VERSION.SDK_INT >= 29) mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
        else mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
