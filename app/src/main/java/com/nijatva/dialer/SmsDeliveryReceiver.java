package com.nijatva.dialer;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/* loaded from: classes.dex */
public class SmsDeliveryReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Bundle extras;
        Object[] objArr;
        if (intent == null || !"android.provider.Telephony.SMS_DELIVER".equals(intent.getAction()) || (extras = intent.getExtras()) == null || (objArr = (Object[]) extras.get("pdus")) == null || objArr.length == 0) {
            return;
        }
        String string = extras.getString("format");
        StringBuilder sb = new StringBuilder();
        String str = null;
        for (Object obj : objArr) {
            SmsMessage createFromPdu = Build.VERSION.SDK_INT >= 23 ? SmsMessage.createFromPdu((byte[]) obj, string) : SmsMessage.createFromPdu((byte[]) obj);
            if (createFromPdu != null) {
                if (str == null) {
                    str = createFromPdu.getOriginatingAddress();
                }
                sb.append(createFromPdu.getDisplayMessageBody() == null ? "" : createFromPdu.getDisplayMessageBody());
            }
        }
        if (str == null) {
            return;
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("address", str);
            contentValues.put("body", sb.toString());
            contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
            contentValues.put("read", (Integer) 0);
            contentValues.put("type", (Integer) 1);
            context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, contentValues);
        } catch (Exception unused) {
        }
    }
}
