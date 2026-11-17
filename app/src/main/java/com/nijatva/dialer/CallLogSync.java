package com.nijatva.dialer;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.provider.CallLog;

/* Writes a call into the system CallLog only when the system itself has not logged it yet */
public class CallLogSync {
    /* call types: 1 = incoming, 2 = outgoing, 3 = missed */
    public static void insertIfMissing(Context ctx, String number, int type, long durationSec) {
        try {
            if (Build.VERSION.SDK_INT >= 23 && ctx.checkSelfPermission("android.permission.WRITE_CALL_LOG") != 0) {
                return;
            }
            String d = SpamUtil.digits(number);
            if (d.length() < 3) {
                return;
            }
            long now = System.currentTimeMillis();
            long since = now - 15000L;
            android.database.Cursor c = null;
            boolean exists = false;
            try {
                c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"_id"},
                        "NUMBER LIKE ? AND DATE >= ?", new String[]{"%" + d, String.valueOf(since)}, "date DESC LIMIT 1");
                if (c != null) {
                    exists = c.moveToFirst();
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            if (exists) {
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put("NUMBER", number);
            cv.put("TYPE", Integer.valueOf(type));
            cv.put("DATE", Long.valueOf(now));
            cv.put("DURATION", Long.valueOf(durationSec));
            cv.put("NEW", Integer.valueOf(type == 3 ? 1 : 0));
            cv.put("CACHE_ONLY", Integer.valueOf(0));
            ctx.getContentResolver().insert(CallLog.Calls.CONTENT_URI, cv);
        } catch (Exception e) {
        }
    }
}
