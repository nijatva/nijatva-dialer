package com.nijatva.dialer;

import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.SecureRandom;

/* loaded from: classes.dex */
public class PinUtils {
    public static boolean isHash(String str) {
        return str != null && str.length() == 64 && str.matches("[0-9a-f]+");
    }

    public static String salted(SharedPreferences sharedPreferences, String str) {
        String string = sharedPreferences.getString("pin_salt", null);
        if (string == null) {
            byte[] bArr = new byte[16];
            new SecureRandom().nextBytes(bArr);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", Byte.valueOf(bArr[i])));
            }
            string = sb.toString();
            sharedPreferences.edit().putString("pin_salt", string).commit();
        }
        return sha256(string + str);
    }

    public static String sha256(String str) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (Exception unused) {
            return null;
        }
    }
}
