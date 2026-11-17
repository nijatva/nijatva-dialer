package com.nijatva.dialer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/* loaded from: classes.dex */
public class DisplayActivity extends Activity {
    private static final int[] ACCENTS = {-15374912, -13730510, -16743537, -9823334, -1086464};
    private static final float[] FONT_SCALES = {0.85f, 1.0f, 1.2f, 1.4f};
    private static final int[] THEME_ROW_IDS = {R.id.rowThemeLight, R.id.rowThemeDark, R.id.rowThemeSystem};
    private static final int[] RADIO_IDS = {R.id.radioLight, R.id.radioDark, R.id.radioSystem};
    private static final int[] LABEL_IDS = {R.id.lblThemeLight, R.id.lblThemeDark, R.id.lblThemeSystem};
    private static final int[] FONT_ROW_IDS = {R.id.rowFontSmall, R.id.rowFontDefault, R.id.rowFontLarge, R.id.rowFontXl};
    private static final int[] RADIO_FONT_IDS = {R.id.radioFont0, R.id.radioFont1, R.id.radioFont2, R.id.radioFont3};
    private static final int[] LABEL_FONT_IDS = {R.id.lblFont0, R.id.lblFont1, R.id.lblFont2, R.id.lblFont3};
    private static final int[] AA_IDS = {R.id.aa0, R.id.aa1, R.id.aa2, R.id.aa3};
    private int themeGen;
    private SharedPreferences prefs;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }


    public void applyState() {
        int i;
        int i2 = 1;
        if (this.prefs.contains("theme_mode")) {
            i2 = this.prefs.getInt("theme_mode", 1);
        } else if (!this.prefs.getBoolean("theme_dark", true)) {
            i2 = 0;
        }
        for (int i3 = 0; i3 < 3; i3++) {
            boolean z = i3 == i2;
            findViewById(RADIO_IDS[i3]).setBackgroundResource(z ? R.drawable.bg_radio_on : R.drawable.bg_radio_off);
            ((TextView) findViewById(LABEL_IDS[i3])).setTextColor(z ? getResources().getColor(R.color.brand_green) : getResources().getColor(R.color.text_hint));
        }
        int i4 = this.prefs.getInt("accent", -15374912);
        for (int i5 = 0; i5 < ACCENTS.length; i5++) {
            findViewById(getResources().getIdentifier("swatch" + i5, "id", getPackageName())).setBackgroundResource(ACCENTS[i5] == i4 ? R.drawable.bg_swatch_selected : 0);
        }
        float f = this.prefs.getFloat("font_scale", 1.0f);
        if (f != 0.85f) {
            i = f == 1.2f ? 2 : f == 1.4f ? 3 : 1;
        } else {
            i = 0;
        }
        for (int i6 = 0; i6 < 4; i6++) {
            boolean z2 = i6 == i;
            findViewById(RADIO_FONT_IDS[i6]).setBackgroundResource(z2 ? R.drawable.bg_radio_on : R.drawable.bg_radio_off);
            ((TextView) findViewById(LABEL_FONT_IDS[i6])).setTextColor(z2 ? getResources().getColor(R.color.brand_green) : getResources().getColor(R.color.text_hint));
            ((TextView) findViewById(AA_IDS[i6])).setTextColor(z2 ? getResources().getColor(R.color.brand_green) : getResources().getColor(R.color.text_hint));
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_display);
        this.prefs = getSharedPreferences("mydialer", 0);
        findViewById(R.id.displayBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                DisplayActivity.this.finish();
            }
        });
        for (int i = 0; i < 3; i++) {
            final int i2 = i;
            findViewById(THEME_ROW_IDS[i]).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    DisplayActivity.this.prefs.edit().putInt("theme_mode", i2).commit();
                    ThemeUtil.generation++;
                    DisplayActivity.this.recreate();
                }
            });
        }
        for (int i3 = 0; i3 < ACCENTS.length; i3++) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            gradientDrawable.setColor(ACCENTS[i3]);
            ((FrameLayout) findViewById(getResources().getIdentifier("swatch" + i3, "id", getPackageName()))).getChildAt(0).setBackground(gradientDrawable);
            final int i4 = ACCENTS[i3];
            findViewById(getResources().getIdentifier("swatch" + i3, "id", getPackageName())).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    DisplayActivity.this.prefs.edit().putInt("accent", i4).commit();
                    DisplayActivity.this.applyState();
                }
            });
        }
        for (int i5 = 0; i5 < 4; i5++) {
            final int i6 = i5;
            findViewById(FONT_ROW_IDS[i5]).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    DisplayActivity.this.prefs.edit().putFloat("font_scale", FONT_SCALES[i6]).commit();
                    ThemeUtil.generation++;
                    DisplayActivity.this.recreate();
                }
            });
        }
        applyState();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
    }
}
