package com.nijatva.dialer;

import android.content.Context;

/* Masks phone numbers for privacy mode */
public class PrivacyUtil {
    public static String mask(Context context, String number) {
        if (number == null) {
            return "";
        }
        try {
            if (!context.getSharedPreferences("mydialer", 0).getBoolean("hide_number", false)) {
                return number;
            }
        } catch (Exception unused) {
            return number;
        }
        String trim = number.trim();
        if (trim.length() <= 5) {
            return "*****";
        }
        return "***** " + trim.substring(trim.length() - 5);
    }
}
