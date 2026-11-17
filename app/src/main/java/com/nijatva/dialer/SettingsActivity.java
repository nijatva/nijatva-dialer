package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class SettingsActivity extends Activity {
    private static final int REQ_EXPORT = 300;
    private static final int REQ_EXPORT_CALLS = 320;
    private static final int REQ_IMPORT = 310;
    private static final int REQ_SET_DEFAULT = 330;
    private static final int REQ_WRITE_CONTACTS = 340;
    private static final int[] SECTION_IDS = {R.id.catAnswerEnd, R.id.catCalls, R.id.catDialpad, R.id.catContacts, R.id.catMore};
    // Donation support. Leave empty in the public source; set your own UPI ID
    // in your private build to enable the "Support the developer" buttons.
    private static final String SUPPORT_UPI_ID = "";
    private SharedPreferences prefs;

    /** Top card with Appearance and Developer entries. */
    private View buildAppearanceAndDeveloperCard() {
        float density = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1E1E1E);
        bg.setCornerRadius(28);
        box.setBackground(bg);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = (int) (density * 12.0f);
        box.setLayoutParams(lp);

        box.addView(nijatvaRow(getString(R.string.appearance_title),
                getString(R.string.appearance_desc), new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this, AppearanceActivity.class));
                    }
                }));
        box.addView(nijatvaRow(getString(R.string.developer_title),
                getString(R.string.developer_desc), new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this, DeveloperActivity.class));
                    }
                }));
        return box;
    }

    private View nijatvaRow(String title, String subtitle, View.OnClickListener click) {
        float density = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.VERTICAL);
        row.setPadding((int) (density * 20.0f), (int) (density * 14.0f),
                (int) (density * 20.0f), (int) (density * 14.0f));
        row.setOnClickListener(click);
        row.setBackgroundColor(0x00000000);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(15);
        row.addView(t);

        TextView d = new TextView(this);
        d.setText(subtitle);
        d.setTextColor(0xFF8E918F);
        d.setTextSize(12);
        row.addView(d);
        return row;
    }

    private String maskNumber(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        if (trim.length() <= 5) {
            return "*****";
        }
        return "***** " + trim.substring(trim.length() - 5);
    }

    /* v141: key by account TYPE only - dedupes multiple account names of the same type */
    private static String accountKey(String str, String str2) {
        return str == null ? "" : str;
    }

    private static String accountLabel(String str, String str2) {
        String lower = str == null ? "" : str.toLowerCase();
        if (lower.contains("whatsapp")) {
            return "WhatsApp Sync";
        }
        if (lower.contains("sim") || lower.contains("icc")) {
            return "SIM";
        }
        if (lower.contains("google")) {
            return "Google Sync";
        }
        if (lower.contains("exchange") || lower.contains("outlook") || lower.contains("yahoo")) {
            return "Email Sync";
        }
        if (lower.contains("phone") || lower.contains("local") || lower.contains("contacts") || lower.contains("device")) {
            return "Device Storage";
        }
        if (str2 != null && str2.contains("@") && !str2.trim().isEmpty()) {
            return str2;
        }
        if (lower.contains(".") && lower.length() > lower.lastIndexOf('.') + 1) {
            String last = lower.substring(lower.lastIndexOf('.') + 1);
            return Character.toUpperCase(last.charAt(0)) + last.substring(1);
        }
        return "Device Storage";
    }


    /* Weak-PIN check: returns null when the PIN is strong enough, otherwise an error message */
    public String pinError(String p) {
        if (p == null || p.length() != 4) {
            return getString(R.string.pin_need_4);
        }
        boolean asc = true;
        boolean desc = true;
        for (int i = 0; i < 3; i++) {
            int d0 = p.charAt(i) - 48;
            int d1 = p.charAt(i + 1) - 48;
            if (d1 != d0 + 1) {
                asc = false;
            }
            if (d1 != d0 - 1) {
                desc = false;
            }
        }
        if (asc || desc) {
            return getString(R.string.pin_sequential);
        }
        int[] cnt = new int[10];
        for (int i2 = 0; i2 < 4; i2++) {
            cnt[p.charAt(i2) - 48]++;
        }
        for (int c : cnt) {
            if (c >= 3) {
                return getString(R.string.pin_repeat);
            }
        }
        return null;
    }

    public void askPin() {
        final android.widget.EditText editText = M3Util.input(this, true, getString(R.string.lock_set));
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editText.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.lock_pin_title);
        title.setTextSize(17.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = M3Util.dp(this, 16);
        card.addView(M3Util.inputWithEye(this, editText), ilp);
        final android.widget.TextView pinErr = new android.widget.TextView(this);
        pinErr.setTextSize(12.0f);
        pinErr.setTextColor(0xffff5252);
        pinErr.setVisibility(8);
        android.widget.LinearLayout.LayoutParams elp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        elp.topMargin = M3Util.dp(this, 6);
        card.addView(pinErr, elp);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final android.widget.TextView saveBtn = M3Util.greenBtn(this, getString(R.string.save_tag), 0);
        android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        slp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(saveBtn, slp);
        card.addView(btnRow, blp);
        saveBtn.setEnabled(false);
        saveBtn.setAlpha(0.4f);
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(android.text.Editable editable) {
                String p = editable.toString();
                String err = null;
                if (p.length() == 4) {
                    err = SettingsActivity.this.pinError(p);
                } else if (p.length() > 0) {
                    err = SettingsActivity.this.getString(R.string.pin_need_4);
                }
                if (err != null) {
                    pinErr.setText(err);
                    pinErr.setVisibility(0);
                    saveBtn.setEnabled(false);
                    saveBtn.setAlpha(0.4f);
                } else if (p.length() == 4) {
                    pinErr.setVisibility(8);
                    saveBtn.setEnabled(true);
                    saveBtn.setAlpha(1.0f);
                } else {
                    pinErr.setVisibility(8);
                }
            }
        });
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String trim = editText.getText().toString().trim();
                String perr = SettingsActivity.this.pinError(trim);
                if (perr != null) {
                    pinErr.setText(perr);
                    pinErr.setVisibility(0);
                    return;
                }
                SettingsActivity.this.getSharedPreferences("mydialer", 0).edit().putString("app_pin", PinUtils.salted(SettingsActivity.this.getSharedPreferences("mydialer", 0), trim)).putString("lock_type", "pin").commit();
                Toast.makeText(SettingsActivity.this, R.string.lock_set_done, 0).show();
                try { dlg.dismiss(); } catch (Exception e) { }
                SettingsActivity.this.askSecurityQuestion();
            }
        });
        M3Util.scrim(dlg);
    }


    public void askSecurityQuestion() {
        final android.widget.EditText q = M3Util.input(this, false, getString(R.string.rec_q_hint));
        final android.widget.EditText a = M3Util.input(this, false, getString(R.string.rec_a_hint));
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.lock_recovery_title);
        title.setTextSize(17.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = M3Util.dp(this, 16);
        card.addView(q, ilp);
        android.widget.LinearLayout.LayoutParams alp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = M3Util.dp(this, 12);
        card.addView(a, alp);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final android.widget.TextView saveBtn = M3Util.greenBtn(this, getString(R.string.lock_recovery_save), 0);
        android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        slp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(saveBtn, slp);
        card.addView(btnRow, blp);
        saveBtn.setEnabled(false);
        saveBtn.setAlpha(0.4f);
        final android.text.TextWatcher recWatcher = new android.text.TextWatcher() {
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence cs, int i2, int i3, int i4) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence cs, int i2, int i3, int i4) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(android.text.Editable editable) {
                /* v142: simple empty check - no word count rules */
                boolean ok = !q.getText().toString().trim().isEmpty() && !a.getText().toString().trim().isEmpty();
                saveBtn.setEnabled(ok);
                saveBtn.setAlpha(ok ? 1.0f : 0.45f);
            }
        };
        q.addTextChangedListener(recWatcher);
        a.addTextChangedListener(recWatcher);
        android.widget.ScrollView recScroll = new android.widget.ScrollView(this);
        recScroll.addView(card);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(recScroll).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String question = q.getText().toString().trim();
                String answer = a.getText().toString().trim().toLowerCase();
                if (question.isEmpty() || answer.isEmpty()) {
                    return;
                }
                SettingsActivity.this.getSharedPreferences("mydialer", 0).edit().putString("lock_question", question).putString("lock_answer", PinUtils.salted(SettingsActivity.this.getSharedPreferences("mydialer", 0), answer)).commit();
                Toast.makeText(SettingsActivity.this, R.string.lock_recovery_saved, 0).show();
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg);
        try {
            dlg.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Exception e) {
        }
    }


    public void verifyBeforeRemove() {
        final android.content.SharedPreferences p = getSharedPreferences("mydialer", 0);
        if (p.getString("app_pattern", null) != null) {
            final PatternView pv = new PatternView(this);
            pv.setAccent(getResources().getColor(R.color.brand_green));
            android.widget.LinearLayout card = M3Util.card(this);
            android.widget.TextView title = new android.widget.TextView(this);
            title.setText(R.string.lock_verify_title);
            title.setTextSize(17.0f);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setTextColor(getResources().getColor(R.color.text_main));
            card.addView(title);
            android.widget.LinearLayout.LayoutParams plp = new android.widget.LinearLayout.LayoutParams(-1, M3Util.dp(this, 260));
            plp.topMargin = M3Util.dp(this, 10);
            card.addView(pv, plp);
            android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
            btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = M3Util.dp(this, 16);
            android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
            btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView okBtn = M3Util.greenBtn(this, getString(R.string.lock_remove_all), 0);
            android.widget.LinearLayout.LayoutParams olp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            olp.leftMargin = M3Util.dp(this, 10);
            btnRow.addView(okBtn, olp);
            card.addView(btnRow, blp);
            final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                }
            });
            okBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    String hash = PinUtils.sha256(p.getString("pin_salt", "") + pv.patternString());
                    if (pv.getCells().size() >= 4 && hash != null && hash.equals(p.getString("app_pattern", null))) {
                        SettingsActivity.this.clearLock();
                        try { dlg.dismiss(); } catch (Exception e) { }
                    } else {
                        pv.clearPattern();
                        Toast.makeText(SettingsActivity.this, R.string.wrong_pin, 0).show();
                    }
                }
            });
            M3Util.scrim(dlg);
            return;
        }
        final android.widget.EditText editText = M3Util.input(this, true, getString(R.string.lock_set));
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editText.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        android.widget.LinearLayout card2 = M3Util.card(this);
        android.widget.TextView title2 = new android.widget.TextView(this);
        title2.setText(R.string.lock_verify_title);
        title2.setTextSize(17.0f);
        title2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title2.setTextColor(getResources().getColor(R.color.text_main));
        card2.addView(title2);
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = M3Util.dp(this, 16);
        card2.addView(M3Util.inputWithEye(this, editText), ilp);
        android.widget.LinearLayout btnRow2 = new android.widget.LinearLayout(this);
        btnRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow2.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp2.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn2 = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow2.addView(cancelBtn2, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView okBtn2 = M3Util.greenBtn(this, getString(R.string.lock_remove_all), 0);
        android.widget.LinearLayout.LayoutParams olp2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        olp2.leftMargin = M3Util.dp(this, 10);
        btnRow2.addView(okBtn2, olp2);
        card2.addView(btnRow2, blp2);
        final android.app.AlertDialog dlg2 = new AlertDialog.Builder(this).setView(card2).create();
        cancelBtn2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg2.dismiss(); } catch (Exception e) { }
            }
        });
        okBtn2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String given = editText.getText().toString().trim();
                String stored = p.getString("app_pin", null);
                boolean ok;
                if (PinUtils.isHash(stored)) {
                    String hash = PinUtils.sha256(p.getString("pin_salt", "") + given);
                    ok = hash != null && hash.equals(stored);
                } else {
                    ok = given.equals(stored);
                }
                if (ok) {
                    SettingsActivity.this.clearLock();
                    try { dlg2.dismiss(); } catch (Exception e) { }
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.wrong_pin, 0).show();
                }
            }
        });
        M3Util.scrim(dlg2);
    }


    public void clearLock() {
        getSharedPreferences("mydialer", 0).edit().remove("app_pin").remove("app_pattern").remove("lock_type").remove("lock_question").remove("lock_answer").commit();
        Toast.makeText(this, R.string.lock_removed, 0).show();
    }


    public void askPattern() {
        final PatternView pv = new PatternView(this);
        pv.setAccent(getResources().getColor(R.color.brand_green));
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 20.0f);
        android.widget.TextView hint = new android.widget.TextView(this);
        hint.setText(R.string.lock_pattern_set);
        hint.setPadding(0, 0, 0, pad / 2);
        box.addView(hint);
        box.addView(pv, new android.widget.LinearLayout.LayoutParams(-1, (int) (getResources().getDisplayMetrics().density * 300.0f)));
        new AlertDialog.Builder(this).setTitle(R.string.lock_type_pattern).setView(box).setPositiveButton(R.string.save_tag, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (pv.getCells().size() < 4) {
                    Toast.makeText(SettingsActivity.this, R.string.lock_pattern_set, 0).show();
                    return;
                }
                final String first = pv.patternString();
                final PatternView pv2 = new PatternView(SettingsActivity.this);
                pv2.setAccent(SettingsActivity.this.getResources().getColor(R.color.brand_green));
                android.widget.LinearLayout box2 = new android.widget.LinearLayout(SettingsActivity.this);
                box2.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.TextView hint2 = new android.widget.TextView(SettingsActivity.this);
                hint2.setText(R.string.lock_pattern_confirm);
                hint2.setPadding(0, 0, 0, pad / 2);
                box2.addView(hint2);
                box2.addView(pv2, new android.widget.LinearLayout.LayoutParams(-1, (int) (SettingsActivity.this.getResources().getDisplayMetrics().density * 300.0f)));
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.lock_type_pattern).setView(box2).setPositiveButton(R.string.save_tag, new DialogInterface.OnClickListener() {
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface2, int i2) {
                        if (pv2.getCells().size() < 4) {
                            Toast.makeText(SettingsActivity.this, R.string.lock_pattern_set, 0).show();
                            return;
                        }
                        if (first.equals(pv2.patternString())) {
                            SettingsActivity.this.getSharedPreferences("mydialer", 0).edit().putString("app_pattern", PinUtils.salted(SettingsActivity.this.getSharedPreferences("mydialer", 0), first)).putString("lock_type", "pattern").commit();
                            Toast.makeText(SettingsActivity.this, R.string.lock_pattern_saved, 0).show();
                            SettingsActivity.this.askSecurityQuestion();
                        } else {
                            Toast.makeText(SettingsActivity.this, R.string.lock_pattern_mismatch, 0).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private void askSmsRole() {
        if (getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            return;
        }
        if (Build.VERSION.SDK_INT < 29) {
            Intent intent = new Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
            intent.putExtra("package", getPackageName());
            startActivity(intent);
        } else {
            RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable("android.app.role.SMS")) {
                startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.SMS"), 810);
                return;
            }
            openDefaultAppsSettings();
        }
    }

    private String callTypeName(int i) {
        return i == 1 ? "Incoming" : i == 2 ? "Outgoing" : i == 3 ? "Missed" : i == 5 ? "Rejected" : "Other";
    }


    public boolean canReadCalls() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CALL_LOG") == 0;
    }

    private boolean canReadContacts() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CONTACTS") == 0;
    }

    private boolean canReadPhoneState() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_PHONE_STATE") == 0;
    }


    public boolean doMerge(List<Long> list) {
        try {
            ArrayList<Long> rawIds = new ArrayList<>();
            Iterator<Long> it = list.iterator();
            while (it.hasNext()) {
                Cursor query = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"_id"}, "contact_id=?", new String[]{String.valueOf(it.next().longValue())}, null);
                if (query != null) {
                    while (query.moveToNext()) {
                        Long rawId = Long.valueOf(query.getLong(0));
                        if (!rawIds.contains(rawId)) {
                            rawIds.add(rawId);
                        }
                    }
                    query.close();
                }
            }
            if (rawIds.size() < 2) {
                return false;
            }
            ArrayList<android.content.ContentProviderOperation> ops = new ArrayList<>();
            for (int i2 = 1; i2 < rawIds.size(); i2++) {
                ops.add(android.content.ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                        .withValue("type", Integer.valueOf(1))
                        .withValue("raw_contact_id1", rawIds.get(0))
                        .withValue("raw_contact_id2", rawIds.get(i2))
                        .build());
            }
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    private void exportCallHistory(Uri uri) {
        try {
            StringBuilder sb = new StringBuilder("date,name,number,type,duration_seconds\n");
            Cursor query = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"date", "name", "number", "type", "duration"}, null, null, "date DESC");
            if (query == null) {
                Toast.makeText(this, "Export failed", 0).show();
                return;
            }
            int i = 0;
            while (query.moveToNext()) {
                String string = query.getString(1);
                String string2 = query.getString(2);
                sb.append(query.getLong(0));
                sb.append(',');
                sb.append(string == null ? "" : string.replace(",", " "));
                sb.append(',');
                if (string2 == null) {
                    string2 = "";
                }
                sb.append(string2);
                sb.append(',');
                sb.append(callTypeName(query.getInt(3)));
                sb.append(',');
                sb.append(query.getLong(4));
                sb.append('\n');
                i++;
            }
            query.close();
            OutputStream openOutputStream = getContentResolver().openOutputStream(uri);
            openOutputStream.write(sb.toString().getBytes("UTF-8"));
            openOutputStream.close();
            Toast.makeText(this, i + " calls exported", 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, "Export failed", 0).show();
        }
    }

    private void exportContacts(Uri uri) {
        try {
            ArrayList arrayList = new ArrayList();
            Cursor query = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "data1"}, null, null, "display_name ASC");
            if (query != null) {
                while (query.moveToNext()) {
                    String string = query.getString(0);
                    String string2 = query.getString(1);
                    if (string != null && string2 != null) {
                        arrayList.add("BEGIN:VCARD\nVERSION:3.0\nFN:" + string + "\nTEL;TYPE=CELL:" + string2 + "\nEND:VCARD");
                    }
                }
                query.close();
            }
            if (arrayList.isEmpty()) {
                Toast.makeText(this, "No contacts to export", 0).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                sb.append((String) it.next());
                sb.append("\r\n");
            }
            OutputStream openOutputStream = getContentResolver().openOutputStream(uri);
            openOutputStream.write(sb.toString().getBytes("UTF-8"));
            openOutputStream.close();
            Toast.makeText(this, arrayList.size() + " contacts exported", 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, "Export failed", 0).show();
        }
    }


    public void mergeDuplicates() {
        if (!canReadContacts()) {
            Toast.makeText(this, R.string.merge_needed, 0).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_CONTACTS"}, REQ_WRITE_CONTACTS);
            return;
        }
        LinkedHashMap scan = scanDuplicates();
        if (scan.isEmpty()) {
            Toast.makeText(this, R.string.no_duplicates, 0).show();
        } else {
            showMergeSheet(scan);
        }
    }

    /* Rescan the contacts provider for duplicate phone numbers */
    public LinkedHashMap scanDuplicates() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        try {
            Cursor query = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"data1", "contact_id"}, null, null, null);
            if (query != null) {
                while (query.moveToNext()) {
                    String string = query.getString(0);
                    long j = query.getLong(1);
                    if (string != null) {
                        StringBuilder sb = new StringBuilder();
                        for (char c : string.toCharArray()) {
                            if (Character.isDigit(c)) {
                                sb.append(c);
                            }
                        }
                        String sb2 = sb.toString();
                        if (sb2.length() >= 7) {
                            LinkedHashSet linkedHashSet = (LinkedHashSet) linkedHashMap.get(sb2);
                            if (linkedHashSet == null) {
                                linkedHashSet = new LinkedHashSet();
                                linkedHashMap.put(sb2, linkedHashSet);
                            }
                            linkedHashSet.add(Long.valueOf(j));
                        }
                    }
                }
                query.close();
            }
        } catch (Exception unused) {
        }
        LinkedHashMap result = new LinkedHashMap();
        for (Object entryObj : linkedHashMap.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            if (((LinkedHashSet) entry.getValue()).size() > 1) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /* M3 merge sheet: search, per-row merge, bottom action bar, background merge with loader */
    public void showMergeSheet(final LinkedHashMap map) {
        final java.util.HashMap<Long, String> idNames = new java.util.HashMap<>();
        try {
            Cursor query = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "contact_id"}, null, null, null);
            if (query != null) {
                while (query.moveToNext()) {
                    long cid = query.getLong(1);
                    String dn = query.getString(0);
                    if (dn != null && !idNames.containsKey(Long.valueOf(cid))) {
                        idNames.put(Long.valueOf(cid), dn);
                    }
                }
                query.close();
            }
        } catch (Exception unused) {
        }
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.merge_duplicates);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.widget.EditText search = M3Util.input(this, false, getString(R.string.search_hint));
        android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = M3Util.dp(this, 12);
        card.addView(search, slp);
        final android.widget.LinearLayout rowsBox = new android.widget.LinearLayout(this);
        rowsBox.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = M3Util.dp(this, 6);
        card.addView(rowsBox, rlp);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView allBtn = M3Util.greenBtn(this, getString(R.string.merge_all), 0);
        android.widget.LinearLayout.LayoutParams alp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        alp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(allBtn, alp);
        card.addView(btnRow, blp);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(card);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(scroll).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        allBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.runMergeBatch(new ArrayList(map.keySet()), map, rowsBox, idNames, search, dlg);
            }
        });
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence cs, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence cs, int i, int i2, int i3) {
                SettingsActivity.this.addMergeRows(rowsBox, map, idNames, cs.toString().trim().toLowerCase(), search, dlg);
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(android.text.Editable ed) {
            }
        });
        addMergeRows(rowsBox, map, idNames, "", search, dlg);
        M3Util.scrim(dlg);
    }

    /* Run merges on a background thread with a loader; ONE toast at the end; rows auto-refresh */
    public void runMergeBatch(final ArrayList nums, final LinkedHashMap map, final android.widget.LinearLayout rowsBox, final java.util.HashMap<Long, String> idNames, final android.widget.EditText search, final android.app.AlertDialog dlg) {
        final android.app.ProgressDialog pd = android.app.ProgressDialog.show(this, "", getString(R.string.merging_contacts), true, false);
        new Thread() {
            @Override // java.lang.Thread
            public void run() {
                int ok = 0;
                for (int i = 0; i < nums.size(); i++) {
                    java.util.LinkedHashSet ids = (java.util.LinkedHashSet) map.get(nums.get(i));
                    if (ids != null && ids.size() > 1 && SettingsActivity.this.doMerge(new ArrayList((java.util.Collection) ids))) {
                        ok++;
                    }
                }
                final int fOk = ok;
                SettingsActivity.this.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public void run() {
                        try { pd.dismiss(); } catch (Exception e) { }
                        Toast.makeText(SettingsActivity.this, fOk > 0 ? R.string.merged_toast : R.string.merge_failed, 0).show();
                        LinkedHashMap rescan = SettingsActivity.this.scanDuplicates();
                        map.clear();
                        map.putAll(rescan);
                        SettingsActivity.this.addMergeRows(rowsBox, map, idNames, search.getText().toString().trim().toLowerCase(), search, dlg);
                        SettingsActivity.this.setResult(-1);
                    }
                });
            }
        }.start();
    }


    public void addMergeRows(android.widget.LinearLayout rowsBox, LinkedHashMap map, java.util.HashMap<Long, String> idNames, String q, final android.widget.EditText search, final android.app.AlertDialog dlg) {
        rowsBox.removeAllViews();
        ArrayList numbers = new ArrayList(map.keySet());
        boolean any = false;
        for (int idx = 0; idx < numbers.size(); idx++) {
            final String num = (String) numbers.get(idx);
            java.util.LinkedHashSet ids = (java.util.LinkedHashSet) map.get(num);
            if (ids == null || ids.size() < 2) {
                continue;
            }
            String nm = null;
            for (Object idObj : ids) {
                String dn = idNames.get((Long) idObj);
                if (dn != null) {
                    nm = dn;
                    break;
                }
            }
            final String dispName = nm != null ? nm : num;
            String lq = q == null ? "" : q;
            if (!lq.isEmpty() && !dispName.toLowerCase().contains(lq) && !num.replace(" ", "").contains(lq.replace(" ", ""))) {
                continue;
            }
            any = true;
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.TextView avatar = new android.widget.TextView(this);
            avatar.setText(dispName.substring(0, 1).toUpperCase());
            avatar.setTextColor(-1);
            avatar.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(0xff2fbf4f);
            avatar.setBackgroundDrawable(g);
            row.addView(avatar, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 40), M3Util.dp(this, 40)));
            android.widget.LinearLayout info = new android.widget.LinearLayout(this);
            info.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.TextView nameTv = new android.widget.TextView(this);
            nameTv.setText(dispName);
            nameTv.setTextSize(15.0f);
            nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            nameTv.setTextColor(getResources().getColor(R.color.text_main));
            info.addView(nameTv);
            android.widget.TextView subTv = new android.widget.TextView(this);
            subTv.setText(PrivacyUtil.mask(this, num) + "  |  " + ids.size() + " contacts");
            subTv.setTextSize(13.0f);
            subTv.setTextColor(getResources().getColor(R.color.text_hint));
            info.addView(subTv);
            android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            ilp.leftMargin = M3Util.dp(this, 12);
            row.addView(info, ilp);
            android.widget.TextView mergeBtn = new android.widget.TextView(this);
            mergeBtn.setText(R.string.merge_btn);
            mergeBtn.setTextSize(13.0f);
            mergeBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            mergeBtn.setTextColor(-1);
            mergeBtn.setGravity(android.view.Gravity.CENTER);
            mergeBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_button_green));
            mergeBtn.setPadding(M3Util.dp(this, 16), M3Util.dp(this, 8), M3Util.dp(this, 16), M3Util.dp(this, 8));
            mergeBtn.setClickable(true);
            final java.util.ArrayList single = new ArrayList();
            single.add(num);
            mergeBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    rowsBox.setTag(null);
                    SettingsActivity.this.runMergeBatch(single, map, rowsBox, idNames, search, dlg);
                }
            });
            row.addView(mergeBtn);
            android.widget.LinearLayout.LayoutParams rlp2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp2.topMargin = M3Util.dp(this, 10);
            rowsBox.addView(row, rlp2);
        }
        if (!any) {
            android.widget.TextView empty = new android.widget.TextView(this);
            empty.setText(map.isEmpty() ? R.string.no_duplicates : R.string.no_matches);
            empty.setTextSize(14.0f);
            empty.setTextColor(getResources().getColor(R.color.text_hint));
            empty.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams elp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            elp.topMargin = M3Util.dp(this, 16);
            rowsBox.addView(empty, elp);
        }
    }

    private void openDefaultAppsSettings() {
        if (getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            return;
        }
        try {
            startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
            Toast.makeText(this, R.string.pick_sms_app, 1).show();
        } catch (Exception unused) {
            Toast.makeText(this, R.string.sms_role_failed, 0).show();
        }
    }


    public void openUpi(int i) {
        if (SUPPORT_UPI_ID.length() == 0) {
            Toast.makeText(this, R.string.no_upi_app, 0).show();
            return;
        }
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("upi://pay?pa=" + SUPPORT_UPI_ID + "&pn=" + Uri.encode("Nijatva Dialer") + "&am=" + i + ".00&cu=INR&tn=" + Uri.encode("Support Nijatva Dialer"))));
        } catch (Exception unused) {
            Toast.makeText(this, R.string.no_upi_app, 0).show();
        }
    }


    public void pickBackground() {
        try {
            startActivityForResult(new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 600);
        } catch (Exception unused) {
            Toast.makeText(this, R.string.bg_failed, 0).show();
        }
    }


    public void requestSmsRole() {
        if (Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.RECEIVE_SMS") == 0) {
            askSmsRole();
        } else {
            requestPermissions(new String[]{"android.permission.RECEIVE_SMS"}, 812);
        }
    }


    private void tintSwitches(View view) {
        if (view instanceof Switch) {
            int accent = this.prefs.getInt("accent", 0xFF1565C0);
            int[][] states = {new int[]{16842912}, new int[]{}};
            try {
                ((Switch) view).getThumbDrawable().setTintList(new android.content.res.ColorStateList(states, new int[]{0xFFFFFFFF, 0xFFFFFFFF}));
                ((Switch) view).getTrackDrawable().setTintList(new android.content.res.ColorStateList(states, new int[]{accent, 0xFF555555}));
            } catch (Exception unused) {
            }
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                tintSwitches(viewGroup.getChildAt(i));
            }
        }
    }

    public void setAsDefaultDialer() {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this, R.string.manual_default_help, 1).show();
            return;
        }
        try {
            RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable("android.app.role.DIALER")) {
                startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.DIALER"), REQ_SET_DEFAULT);
                return;
            }
        } catch (Exception unused) {
        }
        Toast.makeText(this, R.string.manual_default_help, 1).show();
    }


    public void showAccountFilterDialog() {
        if (!canReadContacts()) {
            Toast.makeText(this, "Contacts permission needed", 0).show();
            return;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        LinkedHashMap typeMap = new LinkedHashMap();
        try {
            Cursor query = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"account_type", "account_name"}, null, null, null);
            if (query != null) {
                while (query.moveToNext()) {
                    String string = query.getString(0);
                    String string2 = query.getString(1);
                    String accountKey = accountKey(string, string2);
                    if (!linkedHashMap.containsKey(accountKey)) {
                        linkedHashMap.put(accountKey, accountLabel(string, string2));
                        typeMap.put(accountKey, string == null ? "" : string);
                    }
                }
                query.close();
            }
        } catch (Exception unused) {
        }
        if (linkedHashMap.isEmpty()) {
            Toast.makeText(this, "No contact accounts found", 0).show();
            return;
        }
        final ArrayList arrayList = new ArrayList(linkedHashMap.keySet());
        Set<String> stringSet = this.prefs.getStringSet("accounts", null);
        int size = arrayList.size();
        final boolean[] zArr = new boolean[size];
        boolean z = false;
        for (int i2 = 0; i2 < size; i2++) {
            boolean z2 = stringSet == null || stringSet.contains(arrayList.get(i2));
            zArr[i2] = z2;
            if (!z2) {
                z = true;
            }
        }
        if (stringSet != null && !z) {
            for (int i3 = 0; i3 < size; i3++) {
                zArr[i3] = true;
            }
        }
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.contacts_to_display);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        for (int i4 = 0; i4 < size; i4++) {
            final int fi = i4;
            String key = (String) arrayList.get(i4);
            String type = (String) typeMap.get(key);
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.ImageView icon = new android.widget.ImageView(this);
            int iconRes = iconFor(type);
            icon.setImageResource(iconRes);
            if (iconRes == R.drawable.ic_account) {
                icon.setColorFilter(getResources().getColor(R.color.brand_green));
            }
            row.addView(icon, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 32), M3Util.dp(this, 32)));
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText((String) linkedHashMap.get(key));
            label.setTextSize(15.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 14);
            row.addView(label, llp);
            final android.widget.CheckBox cb = M3Util.check(this);
            cb.setChecked(zArr[i4]);
            cb.setClickable(false);
            row.addView(cb);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    zArr[fi] = !zArr[fi];
                    cb.setChecked(zArr[fi]);
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 12);
            card.addView(row, rlp);
        }
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView okBtn = M3Util.greenBtn(this, getString(R.string.set_default_manual_ok), 0);
        android.widget.LinearLayout.LayoutParams olp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        olp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(okBtn, olp);
        card.addView(btnRow, blp);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(card);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(scroll).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                HashSet hashSet = new HashSet();
                boolean z3 = true;
                for (int i5 = 0; i5 < arrayList.size(); i5++) {
                    if (zArr[i5]) {
                        hashSet.add(arrayList.get(i5));
                    } else {
                        z3 = false;
                    }
                }
                if (z3 || hashSet.isEmpty()) {
                    SettingsActivity.this.prefs.edit().remove("accounts").commit();
                } else {
                    SettingsActivity.this.prefs.edit().putStringSet("accounts", hashSet).commit();
                }
                SettingsActivity.this.setResult(-1);
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg);
    }

    /* icon for a contacts account type */
    public static int iconFor(String type) {
        if (type == null) {
            return R.drawable.ic_account;
        }
        String l = type.toLowerCase();
        if (l.contains("whatsapp")) {
            return R.drawable.ic_whatsapp;
        }
        if (l.contains("telegram")) {
            return R.drawable.ic_telegram;
        }
        if (l.contains("sim")) {
            return R.drawable.ic_sim;
        }
        return R.drawable.ic_account;
    }

    private void showCategories() {
        for (int i : SECTION_IDS) {
            findViewById(i).setVisibility(8);
        }
        findViewById(R.id.mainSettings).setVisibility(0);
        findViewById(R.id.mainFooter).setVisibility(0);
        ((TextView) findViewById(R.id.settingsTitle)).setText(R.string.settings);
    }


    public void showPreferredSimDialog() {
        ArrayList arrayList = new ArrayList();
        final ArrayList arrayList2 = new ArrayList();
        arrayList.add(getString(R.string.sim_auto));
        arrayList2.add(-1);
        int i = 0;
        if (canReadPhoneState()) {
            try {
                TelecomManager telecomManager = (TelecomManager) getSystemService("telecom");
                List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
                if (callCapablePhoneAccounts != null) {
                    int i2 = 0;
                    while (i2 < callCapablePhoneAccounts.size()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("SIM ");
                        int i3 = i2 + 1;
                        sb.append(i3);
                        String sb2 = sb.toString();
                        try {
                            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(callCapablePhoneAccounts.get(i2));
                            if (phoneAccount != null && phoneAccount.getLabel() != null) {
                                sb2 = phoneAccount.getLabel().toString();
                            }
                        } catch (Exception unused) {
                        }
                        arrayList.add(sb2);
                        arrayList2.add(Integer.valueOf(i2));
                        i2 = i3;
                    }
                }
            } catch (Exception unused2) {
            }
        }
        arrayList.add(getString(R.string.sim_ask));
        arrayList2.add(99);
        int i4 = this.prefs.getInt("preferred_sim", -1);
        int i5 = 0;
        while (true) {
            if (i5 >= arrayList2.size()) {
                break;
            }
            if (((Integer) arrayList2.get(i5)).intValue() == i4) {
                i = i5;
                break;
            }
            i5++;
        }
        showSimDialog(arrayList, arrayList2, i);
    }


    public void showSection(int i, int i2) {
        findViewById(R.id.mainSettings).setVisibility(8);
        findViewById(R.id.mainFooter).setVisibility(8);
        for (int i3 : SECTION_IDS) {
            findViewById(i3).setVisibility(8);
        }
        findViewById(i).setVisibility(0);
        ((TextView) findViewById(R.id.settingsTitle)).setText(i2);
    }


    public void updateContactClickLabel() {
        ((TextView) findViewById(R.id.contactClickValue)).setText("details".equals(this.prefs.getString("contact_click", "call")) ? R.string.view_details : R.string.call_action);
    }


    public void updatePrefSimLabel() {
        TextView textView = (TextView) findViewById(R.id.prefSimValue);
        int i = this.prefs.getInt("preferred_sim", -1);
        if (i == 99) {
            textView.setText(R.string.sim_ask);
        } else {
            if (i == -1) {
                textView.setText(R.string.sim_auto);
                return;
            }
            textView.setText("SIM " + (i + 1));
        }
    }


    private void applySwitchTints() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return;
        }
        try {
            int accent = getResources().getColor(R.color.accent_text);
            int off = getResources().getColor(R.color.switch_track_off);
            final android.content.res.ColorStateList track = new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{accent, off});
            final android.content.res.ColorStateList thumb = new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{0xffffffff, 0xffcfcfcf});
            android.view.ViewGroup root = (android.view.ViewGroup) findViewById(android.R.id.content);
            java.util.ArrayDeque<android.view.View> stack = new java.util.ArrayDeque<>();
            for (int i = 0; i < root.getChildCount(); i++) {
                stack.push(root.getChildAt(i));
            }
            while (!stack.isEmpty()) {
                android.view.View v = stack.pop();
                if (v instanceof android.widget.Switch) {
                    ((android.widget.Switch) v).setTrackTintList(track);
                    ((android.widget.Switch) v).setThumbTintList(thumb);
                }
                if (v instanceof android.view.ViewGroup) {
                    android.view.ViewGroup g = (android.view.ViewGroup) v;
                    for (int i = 0; i < g.getChildCount(); i++) {
                        stack.push(g.getChildAt(i));
                    }
                }
            }
        } catch (Throwable unused) {
        }
    }

    private void updateProfileRow() {
        TextView textView = (TextView) findViewById(R.id.profileName);
        TextView textView2 = (TextView) findViewById(R.id.profileNumber);
        TextView textView3 = (TextView) findViewById(R.id.profileAvatar);
        if (textView == null || textView3 == null || textView2 == null) {
            return;
        }
        Bitmap bitmap = null;
        String string = getSharedPreferences("mydialer", 0).getString("profile_name", null);
        try {
            File file = new File(getFilesDir(), "profile.jpg");
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception unused) {
        }
        if (bitmap != null) {
            textView3.setText("");
            textView3.setBackground(new BitmapDrawable(getResources(), bitmap));
            try {
                textView3.setOutlineProvider(new ViewOutlineProvider() {
                    @Override // android.view.ViewOutlineProvider
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                textView3.setClipToOutline(true);
            } catch (Exception unused2) {
            }
        } else {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            gradientDrawable.setColor(-3129167);
            textView3.setBackground(gradientDrawable);
            textView3.setClipToOutline(false);
            if (string == null || string.trim().isEmpty()) {
                textView3.setText("");
                textView3.setBackgroundResource(R.drawable.bg_avatar_placeholder);
            } else {
                textView3.setText(string.trim().substring(0, 1).toUpperCase(Locale.US));
            }
        }
        if (string == null || string.trim().isEmpty()) {
            string = getString(R.string.your_profile);
        }
        textView.setText(string);
        String string2 = getSharedPreferences("mydialer", 0).getString("profile_number", "");
        if (string2 == null || string2.trim().isEmpty()) {
            string2 = getString(R.string.profile_desc);
        }
        if (!string2.equals(getString(R.string.profile_desc)) && this.prefs.getBoolean("hide_number", false)) {
            string2 = maskNumber(string2);
        }
        textView2.setText(string2);
    }


    /* Word count helper for recovery validation */
    private int wordCount(String s) {
        if (s == null) {
            return 0;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return 0;
        }
        return t.split("\\s+").length;
    }

    /* M3 app lock options: change PIN / pattern / remove, icon rows with ripple */
    public void showLockOptionsDialog() {
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.app_lock_title);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        Object[][] opts = {
            {R.drawable.ic_key, getString(R.string.lock_change), 0xff2fbf4f},
            {R.drawable.ic_lock, getString(R.string.lock_change_pattern), 0xff2fbf4f},
            {R.drawable.ic_delete, getString(R.string.lock_remove_all), 0xffff5252}
        };
        for (int i = 0; i < 3; i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageResource(((Integer) opts[i][0]).intValue());
            icon.setColorFilter(((Integer) opts[i][2]).intValue());
            row.addView(icon, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 26), M3Util.dp(this, 26)));
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText((String) opts[i][1]);
            label.setTextSize(16.0f);
            label.setTextColor(fi == 2 ? 0xffff5252 : getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 14);
            row.addView(label, llp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                    if (fi == 0) {
                        SettingsActivity.this.askPin();
                    } else if (fi == 1) {
                        SettingsActivity.this.askPattern();
                    } else {
                        SettingsActivity.this.verifyBeforeRemove();
                    }
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 4);
            card.addView(row, rlp);
        }
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(-1, -2));
        card.addView(btnRow, blp);
        dlg[0] = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg[0].dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg[0]);
    }

    /* M3 app lock type chooser */
    public void showLockTypeDialog() {
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.app_lock_title);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        String[] labels = {getString(R.string.lock_type_pin), getString(R.string.lock_type_pattern)};
        for (int i = 0; i < 2; i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(labels[i]);
            label.setTextSize(16.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            row.addView(label, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                    if (fi == 0) {
                        SettingsActivity.this.askPin();
                    } else {
                        SettingsActivity.this.askPattern();
                    }
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 4);
            card.addView(row, rlp);
        }
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(-1, -2));
        card.addView(btnRow, blp);
        dlg[0] = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg[0].dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg[0]);
    }

    /* M3 on-contact-click dialog with green radios */
    public void showContactClickDialog() {
        final String[] vals = {"call", "details"};
        final String cur = this.prefs.getString("contact_click", "call");
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.on_contact_click);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        String[] labels = {getString(R.string.call_action), getString(R.string.view_details)};
        for (int i = 0; i < 2; i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.RadioButton rb = M3Util.radio(this);
            rb.setChecked(vals[i].equals(cur));
            rb.setClickable(false);
            row.addView(rb);
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(labels[i]);
            label.setTextSize(16.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 12);
            row.addView(label, llp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SettingsActivity.this.prefs.edit().putString("contact_click", vals[fi]).commit();
                    SettingsActivity.this.updateContactClickLabel();
                    SettingsActivity.this.setResult(-1);
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 4);
            card.addView(row, rlp);
        }
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(-1, -2));
        card.addView(btnRow, blp);
        dlg[0] = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg[0].dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg[0]);
    }

    /* M3 preferred SIM dialog with green radios */
    public void showSimDialog(final ArrayList labels, final ArrayList values, int sel) {
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.preferred_sim);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        for (int i = 0; i < labels.size(); i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.RadioButton rb = M3Util.radio(this);
            rb.setChecked(i == sel);
            rb.setClickable(false);
            row.addView(rb);
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText((String) labels.get(i));
            label.setTextSize(16.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 12);
            row.addView(label, llp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SettingsActivity.this.prefs.edit().putInt("preferred_sim", ((Integer) values.get(fi)).intValue()).commit();
                    SettingsActivity.this.updatePrefSimLabel();
                    SettingsActivity.this.setResult(-1);
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 4);
            card.addView(row, rlp);
        }
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(-1, -2));
        card.addView(btnRow, blp);
        dlg[0] = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg[0].dismiss(); } catch (Exception e) { }
            }
        });
        M3Util.scrim(dlg[0]);
    }

    /* M3 sort contacts dialog with green radios and full-row ripple clicks */
    public void showSortDialog() {
        final String[] vals = {"first", "last"};
        final String cur = this.prefs.getString("sort_by", "first");
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.sort_by);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        String[] labels = {getString(R.string.sort_first), getString(R.string.sort_last)};
        for (int i = 0; i < 2; i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.RadioButton rb = M3Util.radio(this);
            rb.setChecked(vals[i].equals(cur));
            rb.setClickable(false);
            row.addView(rb);
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(labels[i]);
            label.setTextSize(16.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 12);
            row.addView(label, llp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SettingsActivity.this.prefs.edit().putString("sort_by", vals[fi]).commit();
                    SettingsActivity.this.updateSortLabel();
                    SettingsActivity.this.setResult(-1);
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                }
            });
            android.widget.LinearLayout.LayoutParams rlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(this, 4);
            card.addView(row, rlp);
        }
        dlg[0] = new AlertDialog.Builder(this).setView(card).create();
        M3Util.scrim(dlg[0]);
    }

    /* M3 info dialog: recording policy + shortcut to the system recorder */
    public void showRecPolicyDialog() {
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.rec_policy_title);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        android.widget.TextView msg = new android.widget.TextView(this);
        msg.setText(R.string.rec_policy_msg);
        msg.setTextSize(14.0f);
        msg.setTextColor(getResources().getColor(R.color.text_hint));
        msg.setLineSpacing(0.0f, 1.2f);
        android.widget.LinearLayout.LayoutParams mlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = M3Util.dp(this, 10);
        card.addView(msg, mlp);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 18);
        android.widget.TextView gotBtn = M3Util.outlineBtn(this, getString(R.string.rec_got));
        btnRow.addView(gotBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView openBtn = M3Util.greenBtn(this, getString(R.string.rec_open), 0);
        android.widget.LinearLayout.LayoutParams olp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        olp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(openBtn, olp);
        card.addView(btnRow, blp);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
        gotBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    SettingsActivity.this.startActivity(new Intent("android.provider.MediaStore.RECORD_SOUND"));
                } catch (Exception e) {
                    try {
                        SettingsActivity.this.getPackageManager().getLaunchIntentForPackage("com.coloros.soundrecorder");
                        Intent ci = SettingsActivity.this.getPackageManager().getLaunchIntentForPackage("com.coloros.soundrecorder");
                        if (ci != null) {
                            SettingsActivity.this.startActivity(ci);
                            return;
                        }
                    } catch (Exception e2) {
                    }
                    Toast.makeText(SettingsActivity.this, R.string.rec_no_recorder, 0).show();
                }
            }
        });
        M3Util.scrim(dlg);
    }


    /* M3 single-number blacklist input dialog */
    public void showAddBlacklistDialog() {
        final android.widget.EditText numInput = M3Util.input(this, false, getString(R.string.blacklist_hint));
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.add_blacklist_t);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = M3Util.dp(this, 16);
        card.addView(numInput, ilp);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView saveBtn = M3Util.greenBtn(this, getString(R.string.lock_recovery_save), 0);
        android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        slp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(saveBtn, slp);
        card.addView(btnRow, blp);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String d = SpamUtil.digits(numInput.getText().toString());
                if (d.length() < 7) {
                    Toast.makeText(SettingsActivity.this, R.string.lock_min, 0).show();
                    return;
                }
                java.util.Set<String> old = SettingsActivity.this.prefs.getStringSet("blocked", null);
                java.util.HashSet<String> set = old == null ? new java.util.HashSet<String>() : new java.util.HashSet<String>(old);
                set.add(d);
                SettingsActivity.this.prefs.edit().putStringSet("blocked", set).commit();
                try { dlg.dismiss(); } catch (Exception e) { }
                Toast.makeText(SettingsActivity.this, R.string.blacklist_added, 0).show();
            }
        });
        M3Util.scrim(dlg);
    }

    public void updateSortLabel() {
        ((TextView) findViewById(R.id.sortValue)).setText("last".equals(this.prefs.getString("sort_by", "first")) ? R.string.sort_last : R.string.sort_first);
    }

    private int themeGen;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 800) {
            updateProfileRow();
            return;
        }
        if (i == 810) {
            if (getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
                Toast.makeText(this, R.string.sms_default_done, 0).show();
                return;
            } else {
                openDefaultAppsSettings();
                return;
            }
        }
        if (i == 700 && i2 == -1 && intent != null && intent.getData() != null) {
            try {
                JSONObject jSONObject = new JSONObject();
                for (Map.Entry<String, ?> entry : getSharedPreferences("mydialer", 0).getAll().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        jSONObject.put(entry.getKey(), (String) value);
                    } else if (value instanceof Boolean) {
                        jSONObject.put(entry.getKey(), (Boolean) value);
                    } else if (value instanceof Integer) {
                        jSONObject.put(entry.getKey(), (Integer) value);
                    } else if (value instanceof Set) {
                        JSONArray jSONArray = new JSONArray();
                        for (Object obj : (Set) value) {
                            if (obj != null) {
                                jSONArray.put(String.valueOf(obj));
                            }
                        }
                        jSONObject.put(entry.getKey(), jSONArray);
                    }
                }
                OutputStream openOutputStream = getContentResolver().openOutputStream(intent.getData());
                openOutputStream.write(jSONObject.toString().getBytes("UTF-8"));
                openOutputStream.close();
                Toast.makeText(this, R.string.backup_done, 0).show();
                return;
            } catch (Exception unused) {
                Toast.makeText(this, R.string.backup_failed, 0).show();
                return;
            }
        }
        if (i == 701 && i2 == -1 && intent != null && intent.getData() != null) {
            try {
                InputStream openInputStream = getContentResolver().openInputStream(intent.getData());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = openInputStream.read(bArr);
                    if (read <= 0) {
                        break;
                    } else {
                        byteArrayOutputStream.write(bArr, 0, read);
                    }
                }
                openInputStream.close();
                JSONObject jSONObject2 = new JSONObject(new String(byteArrayOutputStream.toByteArray(), "UTF-8"));
                SharedPreferences.Editor clear = getSharedPreferences("mydialer", 0).edit().clear();
                Iterator<String> keys = jSONObject2.keys();
                while (keys.hasNext()) {
                    String next = keys.next();
                    Object obj2 = jSONObject2.get(next);
                    if (obj2 instanceof Boolean) {
                        clear.putBoolean(next, ((Boolean) obj2).booleanValue());
                    } else if (obj2 instanceof Integer) {
                        clear.putInt(next, ((Integer) obj2).intValue());
                    } else if (obj2 instanceof JSONArray) {
                        HashSet hashSet = new HashSet();
                        JSONArray jSONArray2 = (JSONArray) obj2;
                        for (int i3 = 0; i3 < jSONArray2.length(); i3++) {
                            hashSet.add(jSONArray2.getString(i3));
                        }
                        clear.putStringSet(next, hashSet);
                    } else {
                        clear.putString(next, String.valueOf(obj2));
                    }
                }
                clear.commit();
                M3Util.infoDialog(this, R.string.backup_done_title, R.string.restore_done);
                return;
            } catch (Exception unused2) {
                Toast.makeText(this, R.string.restore_failed, 0).show();
                return;
            }
        }
        if (i != 600 || i2 != -1 || intent == null || intent.getData() == null) {
            boolean z = true;
            if (i == REQ_SET_DEFAULT) {
                if (Build.VERSION.SDK_INT >= 29) {
                    try {
                        RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
                        if (roleManager == null || !roleManager.isRoleHeld("android.app.role.DIALER")) {
                            z = false;
                        }
                        Toast.makeText(this, z ? R.string.now_default : R.string.not_default, 0).show();
                        return;
                    } catch (Exception unused3) {
                        return;
                    }
                }
                return;
            }
            if (i == REQ_EXPORT_CALLS && i2 == -1 && intent != null && intent.getData() != null) {
                exportCallHistory(intent.getData());
            }
            if (i == REQ_IMPORT && i2 == -1 && intent != null && intent.getData() != null) {
                try {
                    Intent intent2 = new Intent("android.intent.action.VIEW");
                    intent2.setDataAndType(intent.getData(), "text/x-vcard");
                    intent2.addFlags(1);
                    startActivity(intent2);
                } catch (Exception unused4) {
                    Toast.makeText(this, R.string.import_failed, 0).show();
                }
            }
            if (i != REQ_EXPORT || i2 != -1 || intent == null || intent.getData() == null) {
                return;
            }
            exportContacts(intent.getData());
            return;
        }
        try {
            InputStream openInputStream2 = getContentResolver().openInputStream(intent.getData());
            FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), "dialer_bg.jpg"));
            byte[] bArr2 = new byte[8192];
            while (true) {
                int read2 = openInputStream2.read(bArr2);
                if (read2 <= 0) {
                    fileOutputStream.close();
                    openInputStream2.close();
                    Toast.makeText(this, R.string.bg_set, 0).show();
                    return;
                }
                fileOutputStream.write(bArr2, 0, read2);
            }
        } catch (Exception unused5) {
            Toast.makeText(this, R.string.bg_failed, 0).show();
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (findViewById(R.id.mainSettings).getVisibility() != 0) {
            showCategories();
        } else {
            super.onBackPressed();
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_settings);
        applySwitchTints();
        try {
            android.widget.LinearLayout nijatvaBox = (android.widget.LinearLayout) findViewById(R.id.mainSettings);
            if (nijatvaBox != null) nijatvaBox.addView(buildAppearanceAndDeveloperCard(), 0);
        } catch (Exception unused) {
        }
        this.prefs = getSharedPreferences("mydialer", 0);
        tintSwitches(getWindow().getDecorView());
        findViewById(R.id.settingsBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.onBackPressed();
            }
        });
        findViewById(R.id.rowChai).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.openUpi(10);
            }
        });
        findViewById(R.id.rowCoffee).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.openUpi(30);
            }
        });
        findViewById(R.id.rowCustom).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                final android.widget.EditText amt = new android.widget.EditText(SettingsActivity.this);
                amt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                amt.setHint("Enter amount");
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.support_custom_title).setView(amt).setPositiveButton(R.string.support_send, new DialogInterface.OnClickListener() {
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String s = amt.getText().toString().trim();
                        int val = 0;
                        try {
                            val = Integer.parseInt(s);
                        } catch (Exception e) {
                        }
                        if (val < 10) {
                            android.widget.Toast.makeText(SettingsActivity.this, R.string.support_min_error, 0).show();
                            return;
                        }
                        SettingsActivity.this.openUpi(val);
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
            }
        });
        findViewById(R.id.rowSetDefault).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.setAsDefaultDialer();
            }
        });
        findViewById(R.id.rowBlocked).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, (Class<?>) BlockedListActivity.class));
            }
        });
        findViewById(R.id.rowAppLock).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                boolean hasLock = SettingsActivity.this.getSharedPreferences("mydialer", 0).getString("app_pin", null) != null || SettingsActivity.this.getSharedPreferences("mydialer", 0).getString("app_pattern", null) != null;
                if (hasLock && (SettingsActivity.this.getSharedPreferences("mydialer", 0).getString("lock_question", "") == null || SettingsActivity.this.getSharedPreferences("mydialer", 0).getString("lock_question", "").trim().isEmpty())) {
                    Toast.makeText(SettingsActivity.this, R.string.lock_recovery_needed_first, 0).show();
                    SettingsActivity.this.askSecurityQuestion();
                } else if (hasLock) {
                    SettingsActivity.this.showLockOptionsDialog();
                } else {
                    SettingsActivity.this.showLockTypeDialog();
                }
            }
        });
        findViewById(R.id.rowTheme).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, DisplayActivity.class));
                } catch (Exception unused) {
                }
            }
        });
        findViewById(R.id.rowBackup).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("application/json");
                    intent.putExtra("android.intent.extra.TITLE", "mydialer-backup.json");
                    SettingsActivity.this.startActivityForResult(intent, 700);
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, R.string.backup_failed, 0).show();
                }
            }
        });
        findViewById(R.id.rowRestore).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("application/json");
                    SettingsActivity.this.startActivityForResult(intent, 701);
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, R.string.restore_failed, 0).show();
                }
            }
        });
        findViewById(R.id.rowSmsDefault).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.requestSmsRole();
            }
        });
        findViewById(R.id.rowProfile).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.startActivityForResult(new Intent(SettingsActivity.this, (Class<?>) ProfileActivity.class), 800);
            }
        });
        updateProfileRow();
        findViewById(R.id.rowCatSpeed).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, (Class<?>) SpeedDialActivity.class));
            }
        });
        findViewById(R.id.rowCatAnswerEnd).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showSection(R.id.catAnswerEnd, R.string.cat_answer);
            }
        });
        findViewById(R.id.rowCatCalls).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showSection(R.id.catCalls, R.string.cat_calls);
            }
        });
        findViewById(R.id.rowCatDialpad).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showSection(R.id.catDialpad, R.string.cat_dialpad);
            }
        });
        findViewById(R.id.rowCatContacts).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showSection(R.id.catContacts, R.string.cat_contacts);
            }
        });
        findViewById(R.id.rowCatMore).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showSection(R.id.catMore, R.string.cat_more);
            }
        });
        Switch r5 = (Switch) findViewById(R.id.block140Switch);
        r5.setChecked(this.prefs.getBoolean("block_140", false));
        r5.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.getSharedPreferences("mydialer", 0).edit().putBoolean("block_140", ((Switch) view).isChecked()).commit();
            }
        });
        Switch r52 = (Switch) findViewById(R.id.favSwitch);
        r52.setChecked(this.prefs.getBoolean("show_favourites", true));
        r52.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.getSharedPreferences("mydialer", 0).edit().putBoolean("show_favourites", ((Switch) view).isChecked()).commit();
            }
        });
        findViewById(R.id.rowBgPhoto).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (new File(SettingsActivity.this.getFilesDir(), "dialer_bg.jpg").exists()) {
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.bg_title).setItems(new CharSequence[]{SettingsActivity.this.getString(R.string.bg_change), SettingsActivity.this.getString(R.string.bg_remove)}, new DialogInterface.OnClickListener() {
                        @Override // android.content.DialogInterface.OnClickListener
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                SettingsActivity.this.pickBackground();
                            } else {
                                new File(SettingsActivity.this.getFilesDir(), "dialer_bg.jpg").delete();
                                Toast.makeText(SettingsActivity.this, R.string.bg_removed, 0).show();
                            }
                        }
                    }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
                } else {
                    SettingsActivity.this.pickBackground();
                }
            }
        });
        findViewById(R.id.rowFakeCall).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, (Class<?>) FakeCallActivity.class));
            }
        });
        findViewById(R.id.rowCallButtons).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, (Class<?>) CallButtonsActivity.class));
            }
        });
        findViewById(R.id.rowVoicemail).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    String vm = ((android.telephony.TelephonyManager) getSystemService("phone")).getVoiceMailNumber();
                    if (vm == null || vm.trim().isEmpty()) {
                        android.widget.Toast.makeText(SettingsActivity.this, R.string.voicemail_not_set, 0).show();
                    } else {
                        SettingsActivity.this.startActivity(new Intent("android.intent.action.DIAL", android.net.Uri.parse("tel:" + vm)));
                    }
                } catch (Exception unused) {
                }
            }
        });
        findViewById(R.id.rowSound).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    SettingsActivity.this.startActivity(new Intent("android.settings.SOUND_SETTINGS"));
                } catch (Exception unused) {
                }
            }
        });
        findViewById(R.id.rowPopup).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (!android.provider.Settings.canDrawOverlays(SettingsActivity.this)) {
                    Toast.makeText(SettingsActivity.this, R.string.popup_allow_overlay, 1).show();
                    try {
                        SettingsActivity.this.startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + SettingsActivity.this.getPackageName())));
                    } catch (Exception unused) {
                    }
                    return;
                }
                boolean booleanValue = !SettingsActivity.this.prefs.getBoolean("caller_popup", true);
                SettingsActivity.this.prefs.edit().putBoolean("caller_popup", booleanValue).commit();
                Toast.makeText(SettingsActivity.this, booleanValue ? R.string.popup_on : R.string.popup_off, 0).show();
            }
        });
        Switch rejectHidden = (Switch) findViewById(R.id.rejectHiddenSwitch);
        rejectHidden.setChecked(this.prefs.getBoolean("reject_hidden", false));
        rejectHidden.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("reject_hidden", ((Switch) view).isChecked()).commit();
            }
        });
        Switch announceSw = (Switch) findViewById(R.id.announceSwitch);
        announceSw.setChecked(this.prefs.getBoolean("announce_caller", false));
        announceSw.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("announce_caller", ((Switch) view).isChecked()).commit();
            }
        });
        Switch r53 = (Switch) findViewById(R.id.flashSwitch);
        r53.setChecked(this.prefs.getBoolean("flash_alert", true));
        r53.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("flash_alert", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r54 = (Switch) findViewById(R.id.vibrateSwitch);
        r54.setChecked(this.prefs.getBoolean("vibrate_answer", false));
        r54.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("vibrate_answer", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r55 = (Switch) findViewById(R.id.speakerSwitch);
        r55.setChecked(this.prefs.getBoolean("speaker_default", false));
        r55.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("speaker_default", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r56 = (Switch) findViewById(R.id.confirmCallSwitch);
        r56.setChecked(this.prefs.getBoolean("confirm_call", false));
        r56.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("confirm_call", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch hideNumSwitch = (Switch) findViewById(R.id.hideNumberSwitch);
        hideNumSwitch.setChecked(this.prefs.getBoolean("hide_number", false));
        hideNumSwitch.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("hide_number", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.updateProfileRow();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch privateCallSw = (Switch) findViewById(R.id.privateCallSwitch);
        privateCallSw.setChecked(this.prefs.getBoolean("private_calling", false));
        privateCallSw.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("private_calling", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r57 = (Switch) findViewById(R.id.openDialpadSwitch);
        r57.setChecked(this.prefs.getBoolean("open_dialpad_default", false));
        r57.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("open_dialpad_default", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r58 = (Switch) findViewById(R.id.vibrateKeysSwitch);
        r58.setChecked(this.prefs.getBoolean("key_vibrate", true));
        r58.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("key_vibrate", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r59 = (Switch) findViewById(R.id.beepKeysSwitch);
        r59.setChecked(this.prefs.getBoolean("key_beeps", true));
        r59.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("key_beeps", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        Switch r510 = (Switch) findViewById(R.id.photoSwitch);
        r510.setChecked(this.prefs.getBoolean("contact_photos", false));
        r510.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.prefs.edit().putBoolean("contact_photos", ((Switch) view).isChecked()).commit();
                SettingsActivity.this.setResult(-1);
            }
        });
        updateContactClickLabel();
        findViewById(R.id.rowContactClick).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                final String[] strArr = {"call", "details"};
                String[] strArr2 = {SettingsActivity.this.getString(R.string.call_action), SettingsActivity.this.getString(R.string.view_details)};
                boolean equals = "details".equals(SettingsActivity.this.prefs.getString("contact_click", "call"));
                SettingsActivity.this.showContactClickDialog();
            }
        });
        updatePrefSimLabel();
        findViewById(R.id.rowPreferredSim).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showPreferredSimDialog();
            }
        });
        Switch blockScammers = (Switch) findViewById(R.id.blockScammersSwitch);
        if (blockScammers != null) {
            blockScammers.setChecked(this.prefs.getBoolean("block_scammers", true));
            blockScammers.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    SettingsActivity.this.prefs.edit().putBoolean("block_scammers", isChecked).commit();
                }
            });
        }
        Switch blockPromo = (Switch) findViewById(R.id.blockPromoSwitch);
        if (blockPromo != null) {
            blockPromo.setChecked(this.prefs.getBoolean("block_promo", false));
            blockPromo.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    SettingsActivity.this.prefs.edit().putBoolean("block_promo", isChecked).commit();
                }
            });
        }
        findViewById(R.id.rowAddBlacklist).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showAddBlacklistDialog();
            }
        });
        if (getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            findViewById(R.id.rowSmsDefault).setVisibility(8);
        }
        Switch spamContacts = (Switch) findViewById(R.id.spamContactsSwitch);
        if (spamContacts != null) {
            spamContacts.setChecked(this.prefs.getBoolean("spam_to_contacts", false));
            spamContacts.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    SettingsActivity.this.prefs.edit().putBoolean("spam_to_contacts", isChecked).commit();
                }
            });
        }
        Switch recEnable = (Switch) findViewById(R.id.recEnableSwitch);
        if (recEnable != null) {
            recEnable.setChecked(this.prefs.getBoolean("rec_ui_enabled", false));
            recEnable.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    SettingsActivity.this.prefs.edit().putBoolean("rec_ui_enabled", isChecked).commit();
                    if (isChecked) {
                        SettingsActivity.this.showRecPolicyDialog();
                    }
                }
            });
        }
        findViewById(R.id.rowMerge).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.mergeDuplicates();
            }
        });
        findViewById(R.id.rowExportCalls).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (!SettingsActivity.this.canReadCalls()) {
                    Toast.makeText(SettingsActivity.this, "Call log permission needed", 0).show();
                    return;
                }
                try {
                    Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("text/csv");
                    intent.putExtra("android.intent.extra.TITLE", "call_history.csv");
                    SettingsActivity.this.startActivityForResult(intent, SettingsActivity.REQ_EXPORT_CALLS);
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, "Could not open file picker", 0).show();
                }
            }
        });
        updateSortLabel();
        findViewById(R.id.rowSort).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String string = SettingsActivity.this.prefs.getString("sort_by", "first");
                final String[] strArr = {"first", "last"};
                String[] strArr2 = {SettingsActivity.this.getString(R.string.sort_first), SettingsActivity.this.getString(R.string.sort_last)};
                boolean equals = "last".equals(string);
                SettingsActivity.this.showSortDialog();
            }
        });
        findViewById(R.id.rowDisplay).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SettingsActivity.this.showAccountFilterDialog();
            }
        });
        findViewById(R.id.rowImport).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.intent.action.GET_CONTENT");
                    intent.setType("*/*");
                    intent.putExtra("android.intent.extra.MIME_TYPES", new String[]{"text/vcard", "text/x-vcard"});
                    SettingsActivity.this.startActivityForResult(intent, SettingsActivity.REQ_IMPORT);
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, R.string.import_failed, 0).show();
                }
            }
        });
        findViewById(R.id.rowExport).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("text/vcard");
                    intent.putExtra("android.intent.extra.TITLE", "contacts_backup.vcf");
                    SettingsActivity.this.startActivityForResult(intent, SettingsActivity.REQ_EXPORT);
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, "Could not open file picker", 0).show();
                }
            }
        });
        findViewById(R.id.rowFeedback).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    SettingsActivity.this.startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("mailto:" + Uri.encode("Vasudev.work@rediffmail.com"))));
                } catch (Exception unused) {
                    Toast.makeText(SettingsActivity.this, R.string.no_email, 0).show();
                }
            }
        });
        findViewById(R.id.rowAbout).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                showAbout();
            }
        });
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 812) {
            askSmsRole();
        }
    }

    private void showAbout() {
        try {
            View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, null);
            final AlertDialog dlg = new AlertDialog.Builder(SettingsActivity.this).setView(aboutView).create();
            if (dlg.getWindow() != null) {
                try { dlg.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0)); } catch (Exception e2) { }
            }
            View okBtn = aboutView.findViewById(R.id.aboutOk);
            if (okBtn != null) {
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View v) {
                        try { dlg.dismiss(); } catch (Exception e3) { }
                    }
                });
            }
            View emailLink = aboutView.findViewById(R.id.aboutEmail);
            if (emailLink != null) {
                emailLink.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View v) {
                        try {
                            startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("mailto:Vasudev.work@rediffmail.com")));
                        } catch (Exception eMail) {
                            Toast.makeText(SettingsActivity.this, "No email app found", 0).show();
                        }
                    }
                });
            }
            View privacyLink = aboutView.findViewById(R.id.aboutPrivacy);
            if (privacyLink != null) {
                privacyLink.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View v) {
                        try { dlg.dismiss(); } catch (Exception eD) { }
                        try {
                            android.widget.LinearLayout card = M3Util.card(SettingsActivity.this);
                            android.widget.TextView title = new android.widget.TextView(SettingsActivity.this);
                            title.setText(R.string.privacy_policy);
                            title.setTextSize(18.0f);
                            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                            title.setTextColor(SettingsActivity.this.getResources().getColor(R.color.text_main));
                            card.addView(title);
                            android.widget.TextView body = new android.widget.TextView(SettingsActivity.this);
                            body.setText(R.string.privacy_text);
                            body.setTextSize(14.0f);
                            body.setTextColor(SettingsActivity.this.getResources().getColor(R.color.text_hint));
                            body.setLineSpacing(0.0f, 1.2f);
                            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
                            blp.topMargin = M3Util.dp(SettingsActivity.this, 10);
                            card.addView(body, blp);
                            android.widget.TextView okBtn = M3Util.greenBtn(SettingsActivity.this, SettingsActivity.this.getString(R.string.set_default_manual_ok), 0);
                            android.widget.LinearLayout.LayoutParams olp = new android.widget.LinearLayout.LayoutParams(-1, -2);
                            olp.topMargin = M3Util.dp(SettingsActivity.this, 18);
                            card.addView(okBtn, olp);
                            final android.app.AlertDialog pdlg = new AlertDialog.Builder(SettingsActivity.this).setView(card).create();
                            okBtn.setOnClickListener(new View.OnClickListener() {
                                @Override // android.view.View.OnClickListener
                                public void onClick(View v2) {
                                    try { pdlg.dismiss(); } catch (Exception e2) { }
                                }
                            });
                            M3Util.scrim(pdlg);
                        } catch (Exception ePriv) {
                        }
                    }
                });
            }
            View rateLink = aboutView.findViewById(R.id.aboutRate);
            if (rateLink != null) {
                rateLink.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View v) {
                        try {
                            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.nijatva.dialer")));
                        } catch (Exception eRate1) {
                            try {
                                startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.nijatva.dialer")));
                            } catch (Exception eRate2) {
                                Toast.makeText(SettingsActivity.this, R.string.rate_unavailable, 0).show();
                            }
                        }
                    }
                });
            }
            dlg.show();
            try {
                android.view.Window w = dlg.getWindow();
                if (w != null) {
                    android.view.WindowManager.LayoutParams lp = w.getAttributes();
                    lp.dimAmount = 0.65f;
                    w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    w.setAttributes(lp);
                }
            } catch (Exception eDim) {
            }
        } catch (Exception e) {
            try {
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.app_name).setMessage(getString(R.string.about_text) + "\n\n[" + e + "]").setPositiveButton(R.string.set_default_manual_ok, (DialogInterface.OnClickListener) null).show();
            } catch (Exception e4) { }
        }
    }
}
