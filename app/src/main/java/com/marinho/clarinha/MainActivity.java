package com.marinho.clarinha;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.marinho.clarinha.core.Painter;
import com.marinho.clarinha.core.Pet;
import com.marinho.clarinha.core.Pose;
import com.marinho.clarinha.core.Skin;

import java.util.ArrayList;

public final class MainActivity extends Activity {
    private PlayView play;
    private FrameLayout menuLayer;
    private LinearLayout panel;
    private Sfx sfx;
    private float dp;
    private int pendingStep;
    private final Bitmap[] thumbs = new Bitmap[Skin.ALL.length];

    static final int PURPLE = 0xFFA06BFF, INK = 0xFF3A2558, SOFT = 0xFF8A6BB5;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        dp = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        play = new PlayView(this);
        root.addView(play, new FrameLayout.LayoutParams(-1, -1));
        sfx = new Sfx(this);
        sfx.on = Prefs.sound(this);
        play.world.sound = sfx;
        play.setScene(Prefs.scene(this));
        play.setTeam(Prefs.team(this));
        play.listener = p -> { if (menuLayer.getVisibility() == View.VISIBLE) buildMenu(); };

        StarButton btn = new StarButton(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(54), px(54), Gravity.TOP | Gravity.END);
        lp.topMargin = px(16); lp.rightMargin = px(16);
        root.addView(btn, lp);
        btn.setOnClickListener(v -> openMenu());

        menuLayer = new FrameLayout(this);
        menuLayer.setBackgroundColor(0x5A0A081E);
        menuLayer.setVisibility(View.GONE);
        menuLayer.setOnClickListener(v -> closeMenu());
        ScrollView sv = new ScrollView(this);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(18), px(16), px(18), px(22));
        panel.setBackground(round(0xFFFFF7FB, 26, 0, 0));
        panel.setClickable(true);
        sv.addView(panel);
        boolean wide = getResources().getConfiguration().screenWidthDp >= 760;
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Math.min(px(720), getResources().getDisplayMetrics().widthPixels),
                -2, wide ? Gravity.CENTER : (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        plp.topMargin = px(40); plp.bottomMargin = wide ? px(40) : 0;
        menuLayer.addView(sv, plp);
        root.addView(menuLayer, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        immersive();
    }

    int px(float v) { return Math.round(v * dp); }

    void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); }

    @Override protected void onResume() {
        super.onResume();
        play.start();
        if (pendingStep > 0) continueHome(true);
        if (menuLayer.getVisibility() == View.VISIBLE) buildMenu();
    }

    @Override protected void onPause() { super.onPause(); play.stop(); }

    @Override protected void onDestroy() { super.onDestroy(); sfx.release(); }

    @Override public void onBackPressed() {
        if (menuLayer.getVisibility() == View.VISIBLE) closeMenu(); else super.onBackPressed();
    }

    void openMenu() { play.showSelected = true; menuLayer.setVisibility(View.VISIBLE); buildMenu(); }
    void closeMenu() { play.showSelected = false; menuLayer.setVisibility(View.GONE); }

    /* ------------------------------------------------------------------ componentes */
    GradientDrawable round(int color, float r, int strokeCol, float strokeW) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(r * dp);
        if (strokeW > 0) d.setStroke(px(strokeW), strokeCol);
        return d;
    }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return t;
    }

    void label(String s) {
        TextView t = text(s.toUpperCase(), 13, SOFT, true);
        t.setLetterSpacing(.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(16); lp.bottomMargin = px(8);
        panel.addView(t, lp);
    }

    TextView button(String s, int bg, int fg, View.OnClickListener l) {
        TextView t = text(s, 16, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(12), px(13), px(12), px(13));
        t.setBackground(round(bg, 16, 0, 0));
        t.setOnClickListener(l);
        return t;
    }

    LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(r, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    void addToRow(LinearLayout r, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        if (r.getChildCount() > 0) lp.leftMargin = px(10);
        r.addView(v, lp);
    }

    Bitmap thumb(int i) {
        if (thumbs[i] != null) return thumbs[i];
        int w = px(84), h = px(96);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        AndroidG g = new AndroidG().set(c);
        Skin sk = Skin.ALL[i];
        Pose P = new Pose().reset(sk);
        P.legL = -.08f; P.legR = .08f; P.blush = .5f; P.fx = 1.2f; P.wing = 0;
        if (i == 0 || i == 5) P.handR(.5f, 17);
        g.translate(w / 2f, h - 4 * dp); g.scale(.76f * dp, .76f * dp);
        Painter.drawChar(g, sk, P, 1.2f);
        thumbs[i] = bm;
        return bm;
    }

    Bitmap sceneThumb(String k) {
        int w = px(96), h = px(60);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidG g = new AndroidG().set(new Canvas(bm));
        g.scale(dp, dp);
        Painter.paintScene(g, 96, 60, k);
        return bm;
    }

    /* ------------------------------------------------------------------ menu */
    void buildMenu() {
        panel.removeAllViews();
        LinearLayout top = row();
        TextView title = text("Amiguinhas", 22, INK, true);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView x = button("✕", 0xFFF0E3FF, 0xFF5A3A8A, v -> closeMenu());
        x.setPadding(0, 0, 0, 0);
        top.addView(x, new LinearLayout.LayoutParams(px(44), px(44)));

        Pet sel = play.selected != null && play.world.pets.contains(play.selected) ? play.selected : (play.world.pets.isEmpty() ? null : play.world.pets.get(0));
        play.selected = sel;

        label("Trocar a roupinha");
        GridLayout grid = new GridLayout(this);
        int panelW = Math.min(px(720), getResources().getDisplayMetrics().widthPixels) - px(36);
        int cols = Math.max(3, panelW / px(104));
        grid.setColumnCount(cols);
        int cell = (panelW - px(10) * (cols - 1)) / cols;
        for (int i = 0; i < Skin.ALL.length; i++) {
            final int idx = i;
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(px(4), px(6), px(4), px(8));
            boolean on = sel != null && sel.skin == i;
            box.setBackground(round(on ? 0xFFF6EFFF : Color.WHITE, 18, on ? PURPLE : 0x22A06BFF, on ? 3 : 1));
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(thumb(i));
            box.addView(iv, new LinearLayout.LayoutParams(px(84), px(96)));
            TextView n = text(Skin.ALL[i].name, 13, INK, true);
            n.setGravity(Gravity.CENTER);
            box.addView(n);
            box.setOnClickListener(v -> {
                Pet p = play.selected;
                if (p == null) return;
                play.world.changeSkin(p, idx);
                saveTeam();
                buildMenu();
            });
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cell;
            glp.setMargins(0, 0, (i % cols == cols - 1) ? 0 : px(10), px(10));
            grid.addView(box, glp);
        }
        panel.addView(grid);

        label("Turminha");
        LinearLayout r1 = row();
        TextView add = button("+ Chamar mais uma", PURPLE, Color.WHITE, v -> {
            if (play.world.pets.size() >= 5) return;
            boolean[] used = new boolean[Skin.ALL.length];
            for (Pet p : play.world.pets) used[p.skin] = true;
            int s = 0;
            for (int k = 0; k < used.length; k++) if (!used[k]) { s = k; break; }
            play.selected = play.world.addPet(s, play.world.W * (.15f + (float) Math.random() * .7f));
            saveTeam(); buildMenu();
        });
        add.setAlpha(play.world.pets.size() < 5 ? 1 : .45f);
        addToRow(r1, add);
        TextView rem = button("Mandar para casa", 0xFFFFE1EE, 0xFFA2376B, v -> {
            if (play.world.pets.size() <= 1 || play.selected == null) return;
            play.world.removePet(play.selected);
            play.selected = play.world.pets.get(play.world.pets.size() - 1);
            saveTeam(); buildMenu();
        });
        rem.setAlpha(play.world.pets.size() > 1 ? 1 : .45f);
        addToRow(r1, rem);

        label("Cenário");
        LinearLayout r2 = row();
        String[][] sc = {{"noite", "Noite"}, {"dia", "Jardim"}, {"quarto", "Quartinho"}};
        for (String[] s : sc) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(px(6), px(6), px(6), px(6));
            boolean on = s[0].equals(play.scene());
            box.setBackground(round(Color.WHITE, 14, on ? PURPLE : 0x22A06BFF, on ? 3 : 1));
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(sceneThumb(s[0]));
            box.addView(iv, new LinearLayout.LayoutParams(px(96), px(60)));
            box.addView(text(s[1], 13, INK, true));
            box.setOnClickListener(v -> { play.setScene(s[0]); Prefs.setScene(this, s[0]); buildMenu(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = px(10);
            r2.addView(box, lp);
        }

        label("Tela inicial do aparelho");
        boolean on = Prefs.homeOn(this);
        TextView st = text(homeStatus(), 14, 0xFF6B5590, false);
        st.setBackground(round(Color.WHITE, 14, 0, 0));
        st.setPadding(px(14), px(10), px(14), px(10));
        st.setLineSpacing(0, 1.25f);
        panel.addView(st, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout r3 = row();
        ((LinearLayout.LayoutParams) r3.getLayoutParams()).topMargin = px(10);
        addToRow(r3, button(on ? "Desligar passeio" : "Ligar passeio na tela inicial", on ? 0xFFFFE1EE : PURPLE, on ? 0xFFA2376B : Color.WHITE, v -> {
            if (Prefs.homeOn(this)) {
                Prefs.setHomeOn(this, false);
                Intent i = new Intent(this, PetService.class).setAction(PetService.ACTION_STOP);
                try { startService(i); } catch (Exception ignored) { }
                buildMenu();
            } else { pendingStep = 1; continueHome(false); }
        }));
        addToRow(r3, button("Ajustar ícones", 0xFFE7F6FF, 0xFF1F6D96, v -> startSetup()));
        LinearLayout r4 = row();
        ((LinearLayout.LayoutParams) r4.getLayoutParams()).topMargin = px(10);
        r4.setGravity(Gravity.CENTER_VERTICAL);
        r4.addView(text("Quantas na tela inicial:  ", 14, INK, true));
        for (int k = 1; k <= 3; k++) {
            final int n = k;
            boolean selN = Prefs.homeCount(this) == k;
            TextView c = button(String.valueOf(k), selN ? PURPLE : Color.WHITE, selN ? Color.WHITE : INK, v -> {
                Prefs.setHomeCount(this, n); reloadService(); buildMenu();
            });
            c.setPadding(0, px(8), 0, px(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(48), -2);
            lp.rightMargin = px(8);
            r4.addView(c, lp);
        }

        label("Opções");
        LinearLayout r5 = row();
        addToRow(r5, button("Som: " + (sfx.on ? "ligado" : "desligado"), 0xFFE7F6FF, 0xFF1F6D96, v -> {
            sfx.on = !sfx.on; Prefs.setSound(this, sfx.on); if (sfx.on) sfx.play("star"); reloadService(); buildMenu();
        }));

        TextView tips = text("Tocar nela: leva um susto  •  Muitos toques: fica tontinha\n"
                + "Arrastar: levanta pelo capuz e ela balança  •  Soltar no alto: cai e quica\n"
                + "Jogar: arrasta rápido e solta  •  Segurar parada: carinho\n"
                + "Dois toques: pulo de alegria  •  Tocar no fundo: estrelinha para ela pegar", 14, 0xFF6B5590, false);
        tips.setLineSpacing(0, 1.3f);
        tips.setBackground(round(Color.WHITE, 14, 0, 0));
        tips.setPadding(px(14), px(10), px(14), px(10));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.topMargin = px(16);
        panel.addView(tips, tl);
    }

    String homeStatus() {
        boolean ov = Prefs.canOverlay(this), us = Prefs.usageGranted(this);
        int n = Prefs.icons(this).size();
        String s = Prefs.homeOn(this) ? "Passeio ligado: ela aparece só na tela inicial.\n" : "Passeio desligado.\n";
        s += (ov ? "✓" : "✗") + " Sobrepor a outros apps\n";
        s += (us ? "✓" : "✗") + " Acesso aos dados de uso (para sumir dentro dos jogos)\n";
        s += n == 0 ? "Nenhum ícone marcado nesta posição da tela. Toque em \"Ajustar ícones\"." : n + (n == 1 ? " ícone marcado" : " ícones marcados") + " para ela brincar.";
        return s;
    }

    void saveTeam() {
        ArrayList<Integer> l = new ArrayList<>();
        for (Pet p : play.world.pets) l.add(p.skin);
        Prefs.setTeam(this, l);
        reloadService();
    }

    void reloadService() {
        if (!Prefs.homeOn(this)) return;
        try { startService(new Intent(this, PetService.class).setAction(PetService.ACTION_RELOAD)); } catch (Exception ignored) { }
    }

    /* ------------------------------------------------------------------ permissões */
    void continueHome(boolean returning) {
        if (pendingStep == 1) {
            if (!Prefs.canOverlay(this)) {
                if (returning) { pendingStep = 0; toast("A permissão \"Sobrepor a outros apps\" não foi ativada."); buildMenu(); return; }
                toast("Ative \"Permitir sobreposição\" para o Clarinha e Amigas e volte aqui.");
                try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }
                return;
            }
            pendingStep = 2; returning = false;
        }
        if (pendingStep == 2) {
            if (!Prefs.usageGranted(this)) {
                if (returning) { pendingStep = 0; toast("O \"Acesso aos dados de uso\" não foi ativado."); buildMenu(); return; }
                toast("Encontre \"Clarinha e Amigas\" na lista e ative o acesso. Depois volte aqui.");
                try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); } catch (Exception ignored) { }
                return;
            }
            pendingStep = 3;
        }
        if (pendingStep == 3) {
            pendingStep = 0;
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
            }
            Prefs.setHomeOn(this, true);
            try { startForegroundService(new Intent(this, PetService.class).setAction(PetService.ACTION_RELOAD)); } catch (Exception ignored) { }
            if (Prefs.icons(this).isEmpty()) toast("Pronto! Agora toque em \"Ajustar ícones\" para marcar onde ela pode brincar.");
            else toast("Pronto! Volte para a tela inicial para ver a turminha passeando.");
            buildMenu();
        }
    }

    void startSetup() {
        if (!Prefs.canOverlay(this)) { pendingStep = 1; continueHome(false); return; }
        try { startForegroundService(new Intent(this, PetService.class).setAction(PetService.ACTION_SETUP)); } catch (Exception ignored) { }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    /** Botão redondo com estrela. */
    static final class StarButton extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path star = new Path();
        StarButton(Context c) { super(c); setClickable(true); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f, d = getResources().getDisplayMetrics().density;
            p.setStyle(Paint.Style.FILL); p.setColor(0x33FFFFFF); c.drawCircle(w / 2, h / 2, r - d, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * d); p.setColor(0xA6FFFFFF); c.drawCircle(w / 2, h / 2, r - 2 * d, p);
            star.reset();
            float R = r * .55f, ri = R * .45f;
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float rr = i % 2 == 0 ? R : ri, x = w / 2 + (float) Math.cos(a) * rr, y = h / 2 + (float) Math.sin(a) * rr;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
            p.setStyle(Paint.Style.FILL); p.setColor(0xFFFFE066); c.drawPath(star, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(Color.WHITE); p.setStrokeWidth(2 * d); c.drawPath(star, p);
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) { super.onRequestPermissionsResult(code, perms, res); }

    static ViewGroup.LayoutParams wrap() { return new ViewGroup.LayoutParams(-2, -2); }
}
