package com.nijatva.dialer;

import android.content.SharedPreferences;

import com.nijatva.signature.SignatureDeveloperActivity;
import com.nijatva.signature.SignatureTheme;

/**
 * The developer card, from the shared signature kit: the same name, handle, figures
 * and links the other Nijatva apps show, drawn in the dialer's own theme.
 */
public class DeveloperActivity extends SignatureDeveloperActivity {

    private SharedPreferences prefs() {
        return getSharedPreferences("mydialer", 0);
    }

    @Override
    protected int storedTheme() {
        int stored = prefs().contains("theme_mode")
                ? prefs().getInt("theme_mode", 1)
                : (prefs().getBoolean("theme_dark", true) ? 1 : 0);
        switch (stored) {
            case 0:
                return SignatureTheme.LIGHT;
            case 2:
                return SignatureTheme.SYSTEM;
            case 3:
                return SignatureTheme.BLACK;
            default:
                return SignatureTheme.DARK;
        }
    }

    @Override
    protected int storedAccent() {
        int stored = prefs().getInt("accent", SignatureTheme.GREEN);
        for (int value : SignatureTheme.ACCENT_VALUES) {
            if (value == stored) {
                return value;
            }
        }
        return SignatureTheme.GREEN;
    }
}
