package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/* v144: dedicated Message Settings screen (separate from main app settings) */
public class MessageSettingsActivity extends Activity {
    private android.content.SharedPreferences prefs;

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_msg_settings);
        this.prefs = getSharedPreferences("mydialer", 0);
        findViewById(R.id.msgSetBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MessageSettingsActivity.this.finish();
            }
        });
        bindSwitch(R.id.swChips, "msg_chips", true, null);
        bindSwitch(R.id.swOtp, "otp_autodelete", false, new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                MessageSettingsActivity.cleanExpiredOtps(MessageSettingsActivity.this);
            }
        });
        bindSwitch(R.id.swSpam, "msg_spam_block", false, null);
        bindSwitch(R.id.swDlr, "delivery_reports", false, null);
        bindSwipeRow();
        findViewById(R.id.rowDefaultSmsCard).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MessageSettingsActivity.this.askSmsRole();
            }
        });
        findViewById(R.id.btnDefaultSms).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MessageSettingsActivity.this.askSmsRole();
            }
        });
        findViewById(R.id.btnOpenSmsSettings).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Toast.makeText(MessageSettingsActivity.this, "Select SMS app, then choose Nijatva Dialer", 0).show();
                MessageSettingsActivity.this.openDefaultAppsSettings();
            }
        });
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        refreshDefaultStatus();
    }

    @Override // android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 811) {
            String defPkg = Telephony.Sms.getDefaultSmsPackage(this);
            if (defPkg != null && defPkg.equals(getPackageName())) {
                Toast.makeText(this, "Nijatva Dialer is now the default SMS app", 0).show();
                refreshDefaultStatus();
            } else {
                /* role dialog did not grant - open the system Default apps settings directly */
                Toast.makeText(this, "Select SMS app, then choose Nijatva Dialer", 0).show();
                openDefaultAppsSettings();
            }
        }
    }

    /* v148: guaranteed route - system Default apps settings screen */
    private void openDefaultAppsSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS));
            } catch (Exception e2) {
                Toast.makeText(this, "Could not open phone settings", 0).show();
            }
        }
    }

    private void bindSwitch(int id, final String key, boolean def, final Runnable onChange) {
        Switch sw = (Switch) findViewById(id);
        sw.setChecked(this.prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton cb, boolean checked) {
                MessageSettingsActivity.this.prefs.edit().putBoolean(key, checked).commit();
                if (checked && onChange != null) {
                    onChange.run();
                }
            }
        });
    }

    private void bindSwipeRow() {
        findViewById(R.id.rowSwipe).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String cur = MessageSettingsActivity.this.prefs.getString("swipe_action", "delete");
                int sel = "read".equals(cur) ? 1 : 0;
                final String[] vals = {"delete", "read"};
                final android.app.AlertDialog swDlg = new AlertDialog.Builder(MessageSettingsActivity.this)
                        .setTitle(R.string.msg_set_swipe_t)
                        .setSingleChoiceItems(new String[]{
                                MessageSettingsActivity.this.getString(R.string.msg_set_swipe_delete),
                                MessageSettingsActivity.this.getString(R.string.msg_set_swipe_read)}, sel, new DialogInterface.OnClickListener() {
                            @Override // android.content.DialogInterface.OnClickListener
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MessageSettingsActivity.this.prefs.edit().putString("swipe_action", vals[i]).commit();
                                ((TextView) MessageSettingsActivity.this.findViewById(R.id.swipeDesc)).setText(vals[i].equals("read") ? R.string.msg_set_swipe_read : R.string.msg_set_swipe_delete);
                                try { ((AlertDialog) dialogInterface).dismiss(); } catch (Exception e) { }
                            }
                        })
                        .setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null)
                        .create();
                M3Util.dark(swDlg);
            }
        });
    }

    /* v146: current default-SMS state - also disables the button when already default */
    private void refreshDefaultStatus() {
        TextView tv = (TextView) findViewById(R.id.defaultStatus);
        TextView btn = (TextView) findViewById(R.id.btnDefaultSms);
        String defPkg = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = defPkg != null && defPkg.equals(getPackageName());
        tv.setText(isDefault ? R.string.msg_set_default_on : R.string.msg_set_default_off);
        btn.setEnabled(!isDefault);
        btn.setAlpha(isDefault ? 0.45f : 1.0f);
        btn.setText(isDefault ? R.string.msg_set_default_done : R.string.msg_set_default_btn);
    }

    /* v147: robust default-SMS role request with visible feedback and layered fallbacks */
    private void askSmsRole() {
        String defPkg = Telephony.Sms.getDefaultSmsPackage(this);
        if (defPkg != null && defPkg.equals(getPackageName())) {
            Toast.makeText(this, "Nijatva Dialer is already the default SMS app", 0).show();
            refreshDefaultStatus();
            return;
        }
        Toast.makeText(this, "Opening default SMS settings...", 0).show();
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS), 811);
                    return;
                }
            } catch (Exception e) {
            }
        }
        try {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivity(intent);
            return;
        } catch (Exception e2) {
        }
        openDefaultAppsSettings();
    }

    /* v144: auto-delete OTP messages older than 24 hours (background thread) */
    public static void cleanExpiredOtps(final Context context) {
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (!context.getSharedPreferences("mydialer", 0).getBoolean("otp_autodelete", false)) {
                        return;
                    }
                    if (!context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context))) {
                        return;
                    }
                    long cutoff = System.currentTimeMillis() - 86400000L;
                    android.database.Cursor c = context.getContentResolver().query(
                            android.net.Uri.parse("content://sms/inbox"),
                            new String[]{"_id", "address", "body"}, "date < ?", new String[]{String.valueOf(cutoff)}, null);
                    if (c != null) {
                        while (c.moveToNext()) {
                            if (MainActivity.threadCategory(c.getString(1), c.getString(2)) == 1) {
                                try {
                                    context.getContentResolver().delete(
                                            android.net.Uri.parse("content://sms/" + c.getLong(0)), null, null);
                                } catch (Exception e2) {
                                }
                            }
                        }
                        c.close();
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }
}
