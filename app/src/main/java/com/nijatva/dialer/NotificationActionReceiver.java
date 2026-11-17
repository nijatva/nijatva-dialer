package com.nijatva.dialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.Call;

/* loaded from: classes.dex */
public class NotificationActionReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Call primary = CallState.primary();
        if (primary == null) {
            return;
        }
        try {
            if ("com.nijatva.dialer.ANSWER".equals(action)) {
                primary.answer(0);
            } else if ("com.nijatva.dialer.REJECT".equals(action)) {
                try {
                    Call.class.getMethod("reject", new Class[0]).invoke(primary, new Object[0]);
                } catch (NoSuchMethodException unused) {
                    primary.reject(false, null);
                }
            } else if ("com.nijatva.dialer.END".equals(action)) {
                primary.disconnect();
            } else if ("com.nijatva.dialer.CALL_BACK".equals(action)) {
                if (primary.getState() == 2) {
                    return;
                }
                String str = CallState.lastCallNumber;
                if (str != null && !str.isEmpty()) {
                    Intent intent2 = new Intent("android.intent.action.DIAL", Uri.parse("tel:" + Uri.encode(str)));
                    intent2.addFlags(268435456);
                    context.startActivity(intent2);
                }
            }
        } catch (Exception unused2) {
        }
    }
}
