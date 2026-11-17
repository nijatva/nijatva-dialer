package com.nijatva.dialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/* loaded from: classes.dex */
public class PhoneCallReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            return;
        }
        String stringExtra = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String stringExtra2 = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(stringExtra) && stringExtra2 != null && !stringExtra2.trim().isEmpty()) {
            if (!context.getSharedPreferences("mydialer", 0).getBoolean("caller_popup", true)) {
                return;
            }
            if (!Settings.canDrawOverlays(context)) {
                return;
            }
            Intent intent2 = new Intent(context, CallOverlayService.class);
            intent2.putExtra(CallOverlayService.EXTRA_NUMBER, stringExtra2);
            intent2.putExtra(CallOverlayService.EXTRA_NAME, ContactLookupHelper.lookupContactName(context, stringExtra2));
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent2);
                } else {
                    context.startService(intent2);
                }
            } catch (Exception unused) {
            }
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stringExtra)) {
            try {
                context.stopService(new Intent(context, CallOverlayService.class));
            } catch (Exception unused2) {
            }
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(stringExtra)) {
            Intent intent3 = new Intent(context, CallOverlayService.class);
            intent3.putExtra("ended", true);
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent3);
                } else {
                    context.startService(intent3);
                }
            } catch (Exception unused3) {
            }
        }
    }
}
