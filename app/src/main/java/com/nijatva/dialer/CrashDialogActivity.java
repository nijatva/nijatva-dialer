package com.nijatva.dialer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/* Material 3 crash report screen: friendly message + collapsible log + copy/restart */
public class CrashDialogActivity extends Activity {
    private String crashLog = "";
    private boolean logOpen = false;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            java.io.File f = new java.io.File(getFilesDir(), "last_crash.txt");
            if (f.exists()) {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
                StringBuilder sb = new StringBuilder();
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line).append('\n');
                }
                br.close();
                this.crashLog = sb.toString();
                f.delete();
            }
        } catch (Exception e) {
            this.crashLog = "Could not read crash log: " + e;
        }
        if (this.crashLog.isEmpty()) {
            this.crashLog = "No crash log available.";
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xb3000000);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(-1, -2, 17);
        rlp.leftMargin = M3Util.dp(this, 20);
        rlp.rightMargin = M3Util.dp(this, 20);

        LinearLayout card = M3Util.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView icon = new TextView(this);
        icon.setText("!");
        icon.setTextSize(26.0f);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setTextColor(0xffffffff);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(0xffff5252);
        icon.setBackgroundDrawable(cg);
        card.addView(icon, new LinearLayout.LayoutParams(M3Util.dp(this, 52), M3Util.dp(this, 52)));

        TextView title = new TextView(this);
        title.setText(R.string.crash_title);
        title.setTextSize(20.0f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = M3Util.dp(this, 14);
        card.addView(title, tlp);

        TextView msg = new TextView(this);
        msg.setText(R.string.crash_msg);
        msg.setTextSize(14.0f);
        msg.setTextColor(getResources().getColor(R.color.text_hint));
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = M3Util.dp(this, 6);
        card.addView(msg, mlp);

        final TextView detailsBtn = M3Util.outlineBtn(this, getString(R.string.crash_view));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = M3Util.dp(this, 16);
        card.addView(detailsBtn, dlp);

        final ScrollView logScroll = new ScrollView(this);
        logScroll.setVisibility(8);
        GradientDrawable lg = new GradientDrawable();
        lg.setCornerRadius(M3Util.dp(this, 12));
        lg.setColor(0xff141414);
        logScroll.setBackgroundDrawable(lg);
        final TextView logText = new TextView(this);
        logText.setText(this.crashLog);
        logText.setTextSize(11.0f);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextColor(0xffb0b0b0);
        logText.setPadding(M3Util.dp(this, 12), M3Util.dp(this, 12), M3Util.dp(this, 12), M3Util.dp(this, 12));
        logScroll.addView(logText, new ViewGroup.LayoutParams(-1, -2));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, M3Util.dp(this, 260));
        slp.topMargin = M3Util.dp(this, 10);
        card.addView(logScroll, slp);

        detailsBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CrashDialogActivity.this.logOpen = !CrashDialogActivity.this.logOpen;
                logScroll.setVisibility(CrashDialogActivity.this.logOpen ? 0 : 8);
                detailsBtn.setText(CrashDialogActivity.this.logOpen ? R.string.crash_hide : R.string.crash_view);
            }
        });

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 18);
        TextView copyBtn = M3Util.outlineBtn(this, getString(R.string.crash_copy));
        btnRow.addView(copyBtn, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView restartBtn = M3Util.greenBtn(CrashDialogActivity.this, getString(R.string.crash_restart), 0);
        LinearLayout.LayoutParams rblp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        rblp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(restartBtn, rblp);
        card.addView(btnRow, blp);

        TextView closeBtn = new TextView(this);
        closeBtn.setText(R.string.crash_close);
        closeBtn.setTextSize(13.0f);
        closeBtn.setTextColor(getResources().getColor(R.color.text_hint));
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(M3Util.dp(this, 12), M3Util.dp(this, 10), M3Util.dp(this, 12), M3Util.dp(this, 2));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = M3Util.dp(this, 6);
        card.addView(closeBtn, clp);

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) CrashDialogActivity.this.getSystemService("clipboard");
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Nijatva Dialer crash log", CrashDialogActivity.this.crashLog));
                    }
                    Toast.makeText(CrashDialogActivity.this, R.string.crash_copied, 0).show();
                } catch (Exception e) {
                }
            }
        });
        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Intent it = new Intent(CrashDialogActivity.this, MainActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                CrashDialogActivity.this.startActivity(it);
                CrashDialogActivity.this.finish();
            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CrashDialogActivity.this.finishAffinity();
            }
        });

        root.addView(card, rlp);
        setContentView(root);
    }
}
