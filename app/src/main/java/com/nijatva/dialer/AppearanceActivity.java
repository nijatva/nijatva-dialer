package com.nijatva.dialer;

import android.content.SharedPreferences;

import com.nijatva.signature.SignatureAppearanceActivity;
import com.nijatva.signature.SignatureTheme;

/**
 * Appearance, drawn by the suite's signature screen: the same four themes and three
 * accents in every Nijatva app.
 *
 * <p>The dialer already had a theme of its own, so the choice is written to the
 * dialer's own preferences - {@code theme_mode} and {@code accent} in
 * {@code mydialer} - which is what {@link ThemeUtil} and the rest of the app read.
 */
public class AppearanceActivity extends SignatureAppearanceActivity {

    private SharedPreferences prefs() {
        return getSharedPreferences("mydialer", 0);
    }

    @Override
    protected int storedTheme() {
        // The dialer's own numbering predates the suite: 0 Light, 1 Dark, 2 System, 3 Black.
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

    @Override
    protected void store(int theme, int accent) {
        int dialerTheme;
        switch (theme) {
            case SignatureTheme.LIGHT:
                dialerTheme = 0;
                break;
            case SignatureTheme.SYSTEM:
                dialerTheme = 2;
                break;
            case SignatureTheme.BLACK:
                dialerTheme = 3;
                break;
            default:
                dialerTheme = 1;
                break;
        }
        prefs().edit()
                .putInt("theme_mode", dialerTheme)
                .putBoolean("dark_sync_v139", true)
                .putInt("accent", accent)
                .apply();
    }
}
