package com.marinho.clarinha;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.marinho.clarinha.core.Pet;
import com.marinho.clarinha.core.Render;
import com.marinho.clarinha.core.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Mantém a turminha passeando na tela inicial, por cima dos ícones, e some dentro dos outros apps. */
public final class PetService extends Service implements Choreographer.FrameCallback {
    public static final String ACTION_STOP = "com.marinho.clarinha.STOP", ACTION_SETUP = "com.marinho.clarinha.SETUP",
            ACTION_RELOAD = "com.marinho.clarinha.RELOAD";
    static final String CHANNEL = "passeio";

    private WindowManager wm;
    private Context winCtx;
    private final World world = new World();
    private final ArrayList<PetWin> wins = new ArrayList<>();
    private final Handler h = new Handler(Looper.getMainLooper());
    private final HashSet<String> homePkgs = new HashSet<>();
    private Sfx sfx;
    private boolean shown, running, setupMode;
    private SetupView setupView;
    private String fgPkg;
    private long lastQuery;
    private long lastNs;
    private float density;
    int screenW, screenH;
    float topInset, bottomInset;
    private boolean stoppingAfterSetup;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        winCtx = this;
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                android.hardware.display.DisplayManager dm = getSystemService(android.hardware.display.DisplayManager.class);
                Context dc = createDisplayContext(dm.getDisplay(android.view.Display.DEFAULT_DISPLAY));
                winCtx = dc.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
            } catch (Exception e) { winCtx = this; }
        }
        wm = winCtx.getSystemService(WindowManager.class);
        density = getResources().getDisplayMetrics().density;
        sfx = new Sfx(this);
        world.sound = sfx;
        world.overlay = true;
        world.allowStars = false;
        goForeground();
        loadHomePkgs();
        measure();
        h.post(poll);
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Passeio na tela inicial", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, PetService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_star)
                .setContentTitle("A turminha está passeando")
                .setContentText("Ela aparece só na tela inicial.")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Parar", stop).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(11, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(11, n);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String a = intent != null ? intent.getAction() : null;
        sfx.on = Prefs.sound(this);
        if (ACTION_STOP.equals(a)) {
            Prefs.setHomeOn(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_SETUP.equals(a)) {
            stoppingAfterSetup = !Prefs.homeOn(this);
            h.postDelayed(this::showSetup, 700);
            return START_STICKY;
        }
        if (!Prefs.homeOn(this)) { stopSelf(); return START_NOT_STICKY; }
        reloadPets();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        hidePets();
        hideSetup();
        sfx.release();
        super.onDestroy();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        measure();
        if (setupView != null) setupView.reload();
    }

    /* ------------------------------------------------------------------ tela */
    @SuppressWarnings("deprecation")
    private void measure() {
        int w, hgt, top, bottom;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics m = wm.getCurrentWindowMetrics();
            Rect b = m.getBounds();
            w = b.width(); hgt = b.height();
            Insets in = m.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            top = in.top; bottom = in.bottom;
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            w = dm.widthPixels; hgt = dm.heightPixels;
            top = dim("status_bar_height"); bottom = dim("navigation_bar_height");
        }
        screenW = w; screenH = hgt; topInset = top; bottomInset = bottom;
        float iconPx = Prefs.iconDp(this) * density;
        world.setSize(w, hgt, iconPx * 1.3f, hgt - bottom - 2 * density, top + 2 * density, density);
        loadPlats();
    }

    private int dim(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void loadPlats() {
        float iconPx = Prefs.iconDp(this) * density, half = iconPx / 2;
        List<float[]> l = new ArrayList<>();
        for (float[] c : Prefs.icons(this)) {
            float cx = c[0] * screenW, cy = c[1] * screenH;
            l.add(new float[]{cx - half, cy - half, cx + half, cy + half});
        }
        world.setPlats(l);
    }

    private void reloadPets() {
        int[] team = Prefs.team(this);
        int n = Math.min(Prefs.homeCount(this), team.length);
        if (n < 1) n = 1;
        boolean same = world.pets.size() == n;
        for (int i = 0; same && i < n; i++) if (world.pets.get(i).skin != team[i]) same = false;
        if (same) return;
        boolean wasShown = shown;
        hidePets();
        while (world.pets.size() > n) world.pets.remove(world.pets.size() - 1);
        for (int i = 0; i < n; i++) {
            if (i < world.pets.size()) world.pets.get(i).skin = team[i];
            else world.addPet(team[i], world.W * (i + 1) / (n + 1));
        }
        if (wasShown) showPets();
    }

    private void loadHomePkgs() {
        homePkgs.clear();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = getPackageManager();
        for (ResolveInfo r : pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY)) {
            if (r.activityInfo != null && !getPackageName().equals(r.activityInfo.packageName)
                    && !"com.android.settings".equals(r.activityInfo.packageName)) homePkgs.add(r.activityInfo.packageName);
        }
    }

    /* ------------------------------------------------------------------ só na tela inicial */
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            boolean want = Prefs.homeOn(PetService.this) && !setupMode && screenOn() && isHome();
            if (want && world.pets.isEmpty()) reloadPets();
            if (want != shown) { if (want) showPets(); else hidePets(); }
            h.postDelayed(this, shown ? 500 : 800);
        }
    };

    private boolean screenOn() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return (pm == null || pm.isInteractive()) && (km == null || !km.isKeyguardLocked());
    }

    private boolean isHome() {
        if (!Prefs.usageGranted(this)) return false;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return false;
        long now = System.currentTimeMillis();
        long from = lastQuery == 0 ? now - 24L * 3600 * 1000 : lastQuery - 2000;
        try {
            UsageEvents ev = usm.queryEvents(from, now);
            UsageEvents.Event e = new UsageEvents.Event();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() == 1) fgPkg = e.getPackageName();
            }
        } catch (Exception ignored) { }
        lastQuery = now;
        if (homePkgs.isEmpty()) loadHomePkgs();
        return fgPkg != null && homePkgs.contains(fgPkg);
    }

    private static int overlayType() { return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; }

    private WindowManager.LayoutParams baseParams(boolean touchable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(1, 1, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 30) lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        else if (Build.VERSION.SDK_INT >= 28) lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        return lp;
    }

    private void showPets() {
        if (shown || !Prefs.canOverlay(this)) return;
        measure();
        for (Pet p : world.pets) {
            PetWin v = new PetWin(winCtx, p);
            v.lp = baseParams(true);
            v.place();
            try { wm.addView(v, v.lp); wins.add(v); } catch (Exception ignored) { }
        }
        shown = true;
        if (!running) { running = true; lastNs = 0; Choreographer.getInstance().postFrameCallback(this); }
    }

    private void hidePets() {
        for (PetWin v : wins) { try { wm.removeViewImmediate(v); } catch (Exception ignored) { } }
        wins.clear();
        shown = false;
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override public void doFrame(long ns) {
        if (!running) return;
        float dt = lastNs == 0 ? .016f : (ns - lastNs) / 1e9f;
        lastNs = ns;
        world.step(dt);
        world.checkHold(SystemClock.uptimeMillis() / 1000f);
        for (PetWin v : wins) {
            if (v.place()) { try { wm.updateViewLayout(v, v.lp); } catch (Exception ignored) { } }
            v.invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    /** Janela flutuante do tamanho da personagem, que a acompanha. */
    final class PetWin extends View {
        final Pet pet;
        WindowManager.LayoutParams lp;
        final AndroidG g = new AndroidG();
        final float[] b = new float[4];
        int wx, wy;

        PetWin(Context c, Pet p) { super(c); pet = p; }

        boolean place() {
            Render.bounds(world, pet, b);
            int l = (int) Math.floor(Math.max(-world.W, b[0])), t = (int) Math.floor(Math.max(-world.H, b[1]));
            int r = (int) Math.ceil(Math.min(world.W * 2, b[2])), bt = (int) Math.ceil(Math.min(world.H * 2, b[3]));
            int w = Math.max(1, r - l), hh = Math.max(1, bt - t);
            boolean ch = lp.x != l || lp.y != t || lp.width != w || lp.height != hh;
            lp.x = l; lp.y = t; lp.width = w; lp.height = hh; wx = l; wy = t;
            return ch;
        }

        @Override protected void onDraw(Canvas c) {
            g.set(c);
            c.translate(-wx, -wy);
            Render.drawPet(g, world, pet, true, false);
            Render.drawParts(g, world, pet.id);
            Render.drawBubble(g, world, pet);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            int a = e.getActionMasked();
            float tm = e.getEventTime() / 1000f, x = e.getRawX(), y = e.getRawY();
            int id = pet.id * 100 + e.getPointerId(e.getActionIndex());
            switch (a) {
                case MotionEvent.ACTION_DOWN: world.pointerDown(id, x, y, tm); break;
                case MotionEvent.ACTION_MOVE: world.pointerMove(pet.id * 100 + e.getPointerId(0), x, y, tm); break;
                case MotionEvent.ACTION_UP: world.pointerUp(id, x, y, tm, false); break;
                case MotionEvent.ACTION_CANCEL: world.pointerUp(id, x, y, tm, true); break;
                default: break;
            }
            return true;
        }
    }

    /* ------------------------------------------------------------------ ajuste dos ícones */
    private void showSetup() {
        if (setupView != null || !Prefs.canOverlay(this)) return;
        setupMode = true;
        hidePets();
        measure();
        setupView = new SetupView(winCtx, this, this::finishSetup);
        WindowManager.LayoutParams lp = baseParams(true);
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        try { wm.addView(setupView, lp); } catch (Exception e) { setupView = null; setupMode = false; }
    }

    private void hideSetup() {
        if (setupView != null) { try { wm.removeViewImmediate(setupView); } catch (Exception ignored) { } setupView = null; }
    }

    private void finishSetup() {
        hideSetup();
        setupMode = false;
        measure();
        if (stoppingAfterSetup && !Prefs.homeOn(this)) { stopSelf(); return; }
        reloadPets();
    }
}
