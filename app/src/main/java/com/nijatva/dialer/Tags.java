package com.nijatva.dialer;

import android.content.Context;
import android.content.SharedPreferences;

/* loaded from: classes.dex */
public class Tags {
    private static String digitsOnly(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    public static String get(Context context, String str) {
        if (str == null) {
            return null;
        }
        String digitsOnly = digitsOnly(str);
        if (digitsOnly.isEmpty()) {
            return null;
        }
        return prefs(context).getString("tag_" + digitsOnly, null);
    }


    public static boolean isSpam(Context context, String str) {
        String str2 = get(context, str);
        return str2 != null && str2.toLowerCase().contains("spam");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("mydialer", 0);
    }


    public static void set(Context context, String str, String str2) {
        String digitsOnly = digitsOnly(str);
        if (digitsOnly.isEmpty()) {
            return;
        }
        SharedPreferences.Editor edit = prefs(context).edit();
        if (str2 == null || str2.trim().isEmpty()) {
            edit.remove("tag_" + digitsOnly);
        } else {
            edit.putString("tag_" + digitsOnly, str2.trim());
        }
        edit.commit();
    }
}
