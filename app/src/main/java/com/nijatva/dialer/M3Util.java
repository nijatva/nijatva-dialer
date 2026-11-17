package com.nijatva.dialer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/* Small Material-3 style helpers for programmatic dialogs */
public class M3Util {
    public static int dp(Context c, float v) {
        return (int) (c.getResources().getDisplayMetrics().density * v);
    }

    public static EditText input(Context c, boolean password, String hint) {
        EditText et = new EditText(c);
        et.setHint(hint);
        et.setTextSize(15.0f);
        et.setTextColor(c.getResources().getColor(R.color.text_main));
        et.setHintTextColor(c.getResources().getColor(R.color.text_hint));
        et.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.bg_input_outline));
        et.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        et.setSingleLine(true);
        if (password) {
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        }
        return et;
    }

    public static TextView greenBtn(Context c, String label, int icon) {
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextSize(15.0f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(-1);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.bg_button_green));
        if (icon != 0) {
            b.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
            b.setCompoundDrawablePadding(dp(c, 8));
        }
        b.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 12));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    public static TextView outlineBtn(Context c, String label) {
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextSize(15.0f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(c.getResources().getColor(R.color.brand_green));
        b.setGravity(Gravity.CENTER);
        b.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.bg_input_outline));
        b.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 12));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 24);
        l.setPadding(p, p, p, p);
        l.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.bg_dialog_card));
        return l;
    }

    public static LinearLayout inputWithEye(final Context c, final EditText et) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(et, new LinearLayout.LayoutParams(0, -2, 1.0f));
        final ImageView eye = new ImageView(c);
        eye.setImageResource(R.drawable.ic_eye_off);
        eye.setColorFilter(c.getResources().getColor(R.color.icon_gray));
        eye.setPadding(dp(c, 10), 0, dp(c, 6), 0);
        eye.setClickable(true);
        eye.setOnClickListener(new View.OnClickListener() {
            boolean hidden = true;
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                hidden = !hidden;
                eye.setImageResource(hidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);
                et.setInputType(hidden ? (android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) : android.text.InputType.TYPE_CLASS_NUMBER);
                et.setSelection(et.getText().length());
            }
        });
        row.addView(eye, new LinearLayout.LayoutParams(dp(c, 44), dp(c, 44)));
        return row;
    }

    public static android.widget.RadioButton radio(Context c) {
        android.widget.RadioButton rb = new android.widget.RadioButton(c);
        int g = c.getResources().getColor(R.color.brand_green);
        android.content.res.ColorStateList sl = new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{g, 0xff9e9e9e});
        rb.setButtonTintList(sl);
        rb.setPadding(0, dp(c, 10), 0, dp(c, 10));
        return rb;
    }

    public static android.widget.CheckBox check(Context c) {
        android.widget.CheckBox cb = new android.widget.CheckBox(c);
        int g = c.getResources().getColor(R.color.brand_green);
        android.content.res.ColorStateList sl = new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{g, 0xff9e9e9e});
        cb.setButtonTintList(sl);
        return cb;
    }

    public static android.widget.LinearLayout rippleRow(Context c) {
        android.widget.LinearLayout l = new android.widget.LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        android.util.TypedValue tv = new android.util.TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        l.setBackgroundResource(tv.resourceId);
        l.setClickable(true);
        l.setFocusable(true);
        l.setPadding(dp(c, 4), dp(c, 8), dp(c, 4), dp(c, 8));
        return l;
    }

    public static TextView outlineBtnIcon(Context c, String label, int icon) {
        TextView b = outlineBtn(c, label);
        try {
            android.graphics.drawable.Drawable d = c.getResources().getDrawable(icon).mutate();
            d.setColorFilter(c.getResources().getColor(R.color.brand_green), android.graphics.PorterDuff.Mode.SRC_ATOP);
            b.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            b.setCompoundDrawablePadding(dp(c, 8));
        } catch (Exception e) {
        }
        return b;
    }

    /* M3 info dialog: title + message + green OK, 28dp card */
    public static void infoDialog(final Context c, int titleRes, int msgRes) {
        LinearLayout card = card(c);
        TextView title = new TextView(c);
        title.setText(titleRes);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(c.getResources().getColor(R.color.text_main));
        card.addView(title);
        TextView msg = new TextView(c);
        msg.setText(msgRes);
        msg.setTextSize(14.0f);
        msg.setTextColor(c.getResources().getColor(R.color.text_hint));
        msg.setLineSpacing(0.0f, 1.2f);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(c, 10);
        card.addView(msg, mlp);
        TextView okBtn = greenBtn(c, c.getString(R.string.set_default_manual_ok), 0);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, -2);
        olp.topMargin = dp(c, 18);
        card.addView(okBtn, olp);
        final AlertDialog d = new AlertDialog.Builder(c).setView(card).create();
        okBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                try { d.dismiss(); } catch (Exception e) { }
            }
        });
        scrim(d);
    }

    /* v146: standard dark dialog window - #1E1E1E background, 28dp rounded corners */
    public static void dark(AlertDialog d) {
        try {
            d.show();
            Window w = d.getWindow();
            if (w != null) {
                android.view.WindowManager.LayoutParams lp = w.getAttributes();
                lp.dimAmount = 0.6f;
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setAttributes(lp);
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(0xff1e1e1e);
                gd.setCornerRadius(dp(d.getContext(), 28.0f));
                w.setBackgroundDrawable(gd);
            }
        } catch (Exception e) {
        }
    }

    public static void scrim(AlertDialog d) {
        try {
            d.show();
            Window w = d.getWindow();
            if (w != null) {
                android.view.WindowManager.LayoutParams lp = w.getAttributes();
                lp.dimAmount = 0.6f;
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setAttributes(lp);
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            }
        } catch (Exception e) {
        }
    }
}
