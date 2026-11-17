package com.nijatva.dialer;

import android.content.Context;
import android.content.res.Configuration;

/* loaded from: classes.dex */
public class ThemeUtil {
    public static int generation = 0;

    public static Context wrap(Context context) {
        try {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            try {
                configuration.fontScale = context.getSharedPreferences("mydialer", 0).getFloat("font_scale", 1.0f);
            } catch (Exception unused) {
            }
            int i = 1;
            try {
                android.content.SharedPreferences sp = context.getSharedPreferences("mydialer", 0);
                if (sp.contains("theme_mode")) {
                    i = sp.getInt("theme_mode", 1);
                } else {
                    i = sp.getBoolean("theme_dark", true) ? 1 : 0;
                }
                /* v139: one-time sync of the whole app to pure dark mode */
                if (!sp.getBoolean("dark_sync_v139", false)) {
                    sp.edit().putInt("theme_mode", 1).putBoolean("dark_sync_v139", true).commit();
                    i = 1;
                }
            } catch (Exception unused2) {
            }
            if (i == 0) {
                configuration.uiMode = (configuration.uiMode & -49) | 16;
            } else if (i == 1 || i == 3) {
                // 1 = Dark, 3 = Black (AMOLED) - both use the dark theme
                configuration.uiMode = (configuration.uiMode & -49) | 32;
            }
            // i == 2 (System) - leave uiMode untouched so the device setting applies
            return context.createConfigurationContext(configuration);
        } catch (Exception unused3) {
            return context;
        }
    }
}
