package com.marinho.clarinha;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Volta a passear depois que o aparelho reinicia. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) return;
        if (!Prefs.homeOn(c) || !Prefs.canOverlay(c)) return;
        try { c.startForegroundService(new Intent(c, PetService.class).setAction(PetService.ACTION_RELOAD)); } catch (Exception ignored) { }
    }
}
