package com.nijatva.dialer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Appearance screen: theme (System / Dark / Light / Black) and accent colour
 * (Green / Saffron / Indigo). Values are stored locally only.
 */
public class AppearanceActivity extends Activity {

    private static final int BG = 0xFF0B0B0B;
    private static final int CARD = 0xFF141414;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA0A6;
    private static final int SAFFRON = 0xFFC8871A;

    private static final String[] THEME_NAMES = {"System", "Dark", "Light", "Black"};
    private static final int[] THEME_VALUES = {2, 1, 0, 3};

    private static final String[] ACCENT_NAMES = {"Green", "Saffron", "Indigo"};
    private static final int[] ACCENT_VALUES = {0xFF2E7D32, 0xFFC8871A, 0xFF3F51B5};

    private SharedPreferences prefs;
    private LinearLayout themeRow, accentRow;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("mydialer", 0);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        setContentView(buildUi());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private int currentAccent() {
        return prefs.getInt("accent", 0xFF1565C0);
    }

    private int currentTheme() {
        if (prefs.contains("theme_mode")) return prefs.getInt("theme_mode", 1);
        return prefs.getBoolean("theme_dark", true) ? 1 : 0;
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        sv.addView(root, new ViewGroup.LayoutParams(-1, -2));

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, dp(16));
        ImageView back = new ImageView(this);
        back.setImageResource(android.R.drawable.ic_menu_revert);
        back.setColorFilter(TEXT);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        head.addView(back);
        TextView title = new TextView(this);
        title.setText("Appearance");
        title.setTextColor(TEXT);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(14), 0, 0, 0);
        head.addView(title);
        root.addView(head);

        // theme card
        LinearLayout themeCard = card();
        TextView tLabel = new TextView(this);
        tLabel.setText("Theme");
        tLabel.setTextColor(TEXT);
        tLabel.setTextSize(15);
        tLabel.setPadding(0, 0, 0, dp(12));
        themeCard.addView(tLabel);

        themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeCard.addView(themeRow);
        root.addView(themeCard);

        // accent card
        LinearLayout accentCard = card();
        TextView aLabel = new TextView(this);
        aLabel.setText("Accent colour");
        aLabel.setTextColor(TEXT);
        aLabel.setTextSize(15);
        aLabel.setPadding(0, 0, 0, dp(12));
        accentCard.addView(aLabel);

        accentRow = new LinearLayout(this);
        accentRow.setOrientation(LinearLayout.HORIZONTAL);
        accentCard.addView(accentRow);
        root.addView(accentCard);

        // note
        TextView note = new TextView(this);
        note.setText("Saved on this device only. Nothing about your choice leaves this phone.");
        note.setTextColor(MUTED);
        note.setTextSize(12);
        note.setPadding(dp(4), dp(2), dp(4), 0);
        root.addView(note);

        renderChips();
        return sv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF262626);
        c.setBackground(bg);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);
        return c;
    }

    private void renderChips() {
        int accent = currentAccent();
        int theme = currentTheme();

        themeRow.removeAllViews();
        for (int i = 0; i < THEME_NAMES.length; i++) {
            themeRow.addView(chip(THEME_NAMES[i], THEME_VALUES[i] == theme, accent, false));
        }

        accentRow.removeAllViews();
        for (int i = 0; i < ACCENT_NAMES.length; i++) {
            boolean selected = ACCENT_VALUES[i] == accent;
            accentRow.addView(chip(ACCENT_NAMES[i], selected, accent, true));
        }
    }

    private TextView chip(final String label, boolean selected, int accent, final boolean isAccent) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(14);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(18), dp(11), dp(18), dp(11));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(22));
        if (selected) {
            bg.setColor(isAccent ? accent : accent);
            bg.setStroke(dp(2), 0xFFFFFFFF);
            t.setTextColor(Color.WHITE);
        } else {
            bg.setColor(0xFF1E1E1E);
            bg.setStroke(dp(1), 0xFF333333);
            t.setTextColor(0xFFDDDDDD);
        }
        t.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(8);
        t.setLayoutParams(lp);

        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isAccent) {
                    for (int i = 0; i < ACCENT_NAMES.length; i++) {
                        if (ACCENT_NAMES[i].equals(label)) {
                            prefs.edit().putInt("accent", ACCENT_VALUES[i]).commit();
                        }
                    }
                } else {
                    for (int i = 0; i < THEME_NAMES.length; i++) {
                        if (THEME_NAMES[i].equals(label)) {
                            prefs.edit().putInt("theme_mode", THEME_VALUES[i]).commit();
                        }
                    }
                }
                renderChips();
                Toast.makeText(AppearanceActivity.this,
                        "Saved. Reopen the app to apply the new look.", Toast.LENGTH_SHORT).show();
            }
        });
        return t;
    }
}
