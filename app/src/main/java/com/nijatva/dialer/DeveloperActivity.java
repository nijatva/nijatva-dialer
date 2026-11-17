package com.nijatva.dialer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Developer page: profile, links and app information. */
public class DeveloperActivity extends Activity {

    private static final int BG = 0xFF0B0B0B;
    private static final int CARD = 0xFF141414;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA0A6;
    private static final int SAFFRON = 0xFFC8871A;

    private static final String GITHUB = "https://github.com/vasudev-work";
    private static final String SITE = "https://vasudev-work.github.io";
    private static final String EMAIL = "Vasudev.work@rediffmail.com";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        setContentView(buildUi());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

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
        head.setPadding(0, 0, 0, dp(18));
        ImageView back = new ImageView(this);
        back.setImageResource(android.R.drawable.ic_menu_revert);
        back.setColorFilter(TEXT);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        head.addView(back);
        TextView title = new TextView(this);
        title.setText("Developer");
        title.setTextColor(TEXT);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(14), 0, 0, 0);
        head.addView(title);
        root.addView(head);

        // avatar
        TextView avatar = new TextView(this);
        avatar.setText("VD");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(30);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(SAFFRON);
        ring.setStroke(dp(3), 0xFF2A2A2A);
        avatar.setBackground(ring);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(96), dp(96));
        alp.gravity = Gravity.CENTER_HORIZONTAL;
        alp.bottomMargin = dp(14);
        avatar.setLayoutParams(alp);
        root.addView(avatar);

        // name + handle
        TextView name = new TextView(this);
        name.setText("Vasudev Dangaya");
        name.setTextColor(TEXT);
        name.setTextSize(24);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        root.addView(name);

        TextView handle = new TextView(this);
        handle.setText("@vasudev-work");
        handle.setTextColor(MUTED);
        handle.setTextSize(14);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(0, dp(4), 0, dp(16));
        root.addView(handle);

        TextView bio = new TextView(this);
        bio.setText("Builds Android apps without Google Play Services.\n\n"
                + "Nijatva is the current project: small, single-purpose apps - "
                + "no accounts, no ads, no analytics.");
        bio.setTextColor(0xFFCCCCCC);
        bio.setTextSize(14);
        bio.setGravity(Gravity.CENTER);
        bio.setPadding(dp(8), 0, dp(8), dp(18));
        root.addView(bio);

        // stats
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(CARD);
        sbg.setCornerRadius(dp(16));
        stats.setBackground(sbg);
        stats.setPadding(dp(8), dp(16), dp(8), dp(16));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.bottomMargin = dp(18);
        stats.setLayoutParams(slp);
        stats.addView(stat("1.0", "Version"));
        stats.addView(divider());
        stats.addView(stat("5.0+", "Android"));
        stats.addView(divider());
        stats.addView(stat("0", "Internet permission"));
        root.addView(stats);

        // links
        root.addView(linkRow("\u25C9", "India", null));
        root.addView(linkRow("\u25CE", "vasudev-work.github.io", SITE));
        root.addView(linkRow("\u2709", EMAIL, "mailto:" + EMAIL));

        // github button
        Button gh = new Button(this);
        gh.setText("View GitHub Profile");
        gh.setAllCaps(false);
        gh.setTextColor(Color.WHITE);
        gh.setTextSize(15);
        GradientDrawable gb = new GradientDrawable();
        gb.setColor(SAFFRON);
        gb.setCornerRadius(dp(26));
        gh.setBackground(gb);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, dp(50));
        glp.topMargin = dp(14);
        glp.bottomMargin = dp(20);
        gh.setLayoutParams(glp);
        gh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { open(GITHUB); }
        });
        root.addView(gh);

        // footer
        TextView footer = new TextView(this);
        footer.setText("Nijatva\nSmall, single-purpose Android apps with no Google Play Services, "
                + "no accounts, no ads and no analytics. Built in India.");
        footer.setTextColor(MUTED);
        footer.setTextSize(12);
        footer.setPadding(dp(4), dp(6), dp(4), 0);
        root.addView(footer);

        return sv;
    }

    private LinearLayout stat(String value, String label) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(TEXT);
        v.setTextSize(22);
        v.setTypeface(null, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        c.addView(v);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(MUTED);
        l.setTextSize(11);
        l.setGravity(Gravity.CENTER);
        c.addView(l);
        return c;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0xFF262626);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(34));
        lp.gravity = Gravity.CENTER_VERTICAL;
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout linkRow(String icon, String text, final String url) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(10), dp(6), dp(10));

        TextView i = new TextView(this);
        i.setText(icon);
        i.setTextColor(SAFFRON);
        i.setTextSize(15);
        i.setPadding(0, 0, dp(12), 0);
        row.addView(i);

        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(url == null ? MUTED : 0xFFE0E0E0);
        t.setTextSize(14);
        row.addView(t);

        if (url != null) {
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { open(url); }
            });
        }
        return row;
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to open this link", Toast.LENGTH_SHORT).show();
        }
    }
}
