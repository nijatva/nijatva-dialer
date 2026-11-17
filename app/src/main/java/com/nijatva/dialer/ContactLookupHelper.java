package com.nijatva.dialer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/* loaded from: classes.dex */
public class ContactLookupHelper {
    public static String lookupContactName(Context context, String phoneNumber) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber)), new String[]{"display_name"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String string = cursor.getString(0);
                cursor.close();
                return string;
            }
        } catch (Exception unused) {
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }
}
