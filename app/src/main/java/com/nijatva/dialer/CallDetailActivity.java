package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
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
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes.dex */
public class CallDetailActivity extends Activity {
    private static final int[] AVATAR_COLORS = {-8812853, -11684180, -6982195, -1023342, -10177034, -18611, -8271996, -749647, -6190977, -11677471};
    private String number = "";
    private String phoneAccount = null;
    private String contactName = null;
    private long contactId = -1;
    private boolean blocked = false;

    /* M3 category chooser for marking a number as spam */
    private void showSpamCategoryChooser() {
        final String[] cats = {SpamUtil.CAT_SPAMMER, SpamUtil.CAT_FRAUD, SpamUtil.CAT_TELE, SpamUtil.CAT_ROBO};
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.pick_category);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
        for (int i = 0; i < cats.length; i++) {
            final int fi = i;
            android.widget.LinearLayout row = M3Util.rippleRow(this);
            android.widget.RadioButton rb = M3Util.radio(this);
            rb.setChecked(i == 0);
            rb.setClickable(false);
            row.addView(rb);
            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(cats[i]);
            label.setTextSize(16.0f);
            label.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams llp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            llp.leftMargin = M3Util.dp(this, 12);
            row.addView(label, llp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg[0].dismiss(); } catch (Exception e) { }
                    SpamUtil.addWithCategory(CallDetailActivity.this, CallDetailActivity.this.number, cats[fi]);
                    android.widget.Toast.makeText(CallDetailActivity.this, R.string.spam_saved, 0).show();
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

    /* M3 series (prefix) marking dialog with custom label */
    private void showSeriesDialog() {
        String d = SpamUtil.digits(this.number);
        String suggest = d.length() > 5 ? d.substring(0, 5) : d;
        final android.widget.EditText prefixInput = M3Util.input(this, false, getString(R.string.series_hint));
        prefixInput.setText(suggest);
        android.widget.LinearLayout card = M3Util.card(this);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.mark_series);
        title.setTextSize(18.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        card.addView(title);
        android.widget.LinearLayout.LayoutParams ilp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = M3Util.dp(this, 16);
        card.addView(prefixInput, ilp);
        final android.widget.EditText labelInput = M3Util.input(this, false, "Label (e.g. Jio Promo)");
        labelInput.setText(SpamUtil.CAT_TELE);
        android.widget.LinearLayout.LayoutParams llp0 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        llp0.topMargin = M3Util.dp(this, 10);
        card.addView(labelInput, llp0);
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = M3Util.dp(this, 20);
        android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
        btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView nextBtn = M3Util.greenBtn(this, getString(R.string.lock_recovery_save), 0);
        android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        nlp.leftMargin = M3Util.dp(this, 10);
        btnRow.addView(nextBtn, nlp);
        card.addView(btnRow, blp);
        final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try { dlg.dismiss(); } catch (Exception e) { }
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String p = SpamUtil.digits(prefixInput.getText().toString());
                String lbl = labelInput.getText().toString().trim();
                if (p.length() < 2 || lbl.isEmpty()) {
                    android.widget.Toast.makeText(CallDetailActivity.this, R.string.lock_min, 0).show();
                    return;
                }
                SpamUtil.addPattern(CallDetailActivity.this, p, lbl);
                try { dlg.dismiss(); } catch (Exception e) { }
                android.widget.Toast.makeText(CallDetailActivity.this, R.string.series_saved, 0).show();
            }
        });
        M3Util.scrim(dlg);
    }

    private void bindActions() {
        this.blocked = isBlocked();
        updateBlockUi();
        findViewById(R.id.detailCallButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23 && CallDetailActivity.this.checkSelfPermission("android.permission.CALL_PHONE") != 0) {
                    CallDetailActivity.this.requestPermissions(new String[]{"android.permission.CALL_PHONE"}, 103);
                    return;
                }
                try {
                    try {
                        boolean hideOn = CallDetailActivity.this.getSharedPreferences("mydialer", 0).getBoolean("private_calling", false);
                        String dialNum = hideOn ? "*31#" + CallDetailActivity.this.number : CallDetailActivity.this.number;
                        if (hideOn) {
                            android.widget.Toast.makeText(CallDetailActivity.this, R.string.private_call_toast, 0).show();
                        }
                        Intent callIntent = new Intent("android.intent.action.CALL", Uri.parse("tel:" + Uri.encode(dialNum)));
                        try {
                            android.telecom.TelecomManager tm = (android.telecom.TelecomManager) CallDetailActivity.this.getSystemService("telecom");
                            if (tm != null && CallDetailActivity.this.phoneAccount != null) {
                                for (android.telecom.PhoneAccountHandle ph : tm.getCallCapablePhoneAccounts()) {
                                    if (CallDetailActivity.this.phoneAccount.equals(ph.getId())) {
                                        callIntent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", ph);
                                        break;
                                    }
                                }
                            }
                        } catch (Exception unusedP) {
                        }
                        CallDetailActivity.this.startActivity(callIntent);
                    } catch (Exception unused) {
                    }
                } catch (Exception unused2) {
                    CallDetailActivity.this.startActivity(new Intent("android.intent.action.DIAL", Uri.parse("tel:" + Uri.encode(CallDetailActivity.this.number))));
                }
            }
        });
        findViewById(R.id.detailSmsButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    CallDetailActivity.this.startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + CallDetailActivity.this.number)));
                } catch (Exception unused) {
                }
            }
        });
        findViewById(R.id.detailEditButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    if (CallDetailActivity.this.contactId > 0) {
                        CallDetailActivity.this.startActivity(new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, CallDetailActivity.this.contactId)));
                    } else {
                        Intent intent = new Intent("android.intent.action.INSERT");
                        intent.setType("vnd.android.cursor.dir/raw_contact");
                        intent.putExtra("phone", CallDetailActivity.this.number);
                        CallDetailActivity.this.startActivity(intent);
                    }
                } catch (Exception unused) {
                }
            }
        });
        findViewById(R.id.detailBlockButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.toggleBlock();
            }
        });
        findViewById(R.id.detailWaButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.openWhatsApp();
            }
        });
        findViewById(R.id.detailTgButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.openTelegram();
            }
        });
        findViewById(R.id.detailArButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.openArattai();
            }
        });
        findViewById(R.id.detailEmailButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.openEmail();
            }
        });
        findViewById(R.id.detailTagButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.showTagDialog();
            }
        });
        findViewById(R.id.detailWabButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.openWhatsAppBusiness();
            }
        });
        try {
            findViewById(R.id.detailVideoButton).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        Intent videoIntent = new Intent("com.whatsapp.video.call");
                        videoIntent.setData(Uri.parse("tel:" + CallDetailActivity.this.number));
                        CallDetailActivity.this.startActivity(videoIntent);
                    } catch (Exception e) {
                        try {
                            CallDetailActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + CallDetailActivity.this.international())).addFlags(268435456));
                        } catch (Exception e2) {
                        }
                    }
                }
            });
        } catch (Exception eVideo) {
        }
        try {
            findViewById(R.id.detailStarButton).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        if (CallDetailActivity.this.contactId <= 0) {
                            android.widget.Toast.makeText(CallDetailActivity.this, "Save the contact first", 0).show();
                            return;
                        }
                        boolean fav = false;
                        try {
                            android.database.Cursor c = CallDetailActivity.this.getContentResolver().query(android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, CallDetailActivity.this.contactId), new String[]{"starred"}, null, null, null);
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    fav = c.getInt(0) == 1;
                                }
                                c.close();
                            }
                        } catch (Exception eq) {
                        }
                        try {
                            android.content.ContentValues cv = new android.content.ContentValues();
                            cv.put("starred", fav ? Integer.valueOf(0) : Integer.valueOf(1));
                            CallDetailActivity.this.getContentResolver().update(android.provider.ContactsContract.Contacts.CONTENT_URI, cv, "_id=?", new String[]{String.valueOf(CallDetailActivity.this.contactId)});
                            android.widget.ImageButton starBtn = (android.widget.ImageButton) CallDetailActivity.this.findViewById(R.id.detailStarButton);
                            starBtn.setColorFilter(CallDetailActivity.this.getResources().getColor(fav ? R.color.text_hint : R.color.brand_green));
                            android.widget.Toast.makeText(CallDetailActivity.this, fav ? "Removed from favourites" : "Added to favourites", 0).show();
                            if (fav) {
                                if (EmojiUtil.FAV.equals(EmojiUtil.get(CallDetailActivity.this, CallDetailActivity.this.number))) {
                                    EmojiUtil.set(CallDetailActivity.this, CallDetailActivity.this.number, "");
                                }
                            } else {
                                EmojiUtil.set(CallDetailActivity.this, CallDetailActivity.this.number, EmojiUtil.FAV);
                            }
                        } catch (Exception eu) {
                        }
                    } catch (Exception eStar) {
                    }
                }
            });
        } catch (Exception eStar2) {
        }
        try {
            findViewById(R.id.detailMenuButton).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        View menuView = CallDetailActivity.this.getLayoutInflater().inflate(R.layout.dialog_menu_card, null);
                        TextView menuBlockLabel = (TextView) menuView.findViewById(R.id.menuBlocklist);
                        if (menuBlockLabel != null) {
                            menuBlockLabel.setText(CallDetailActivity.this.blocked ? R.string.remove_from_blocklist : R.string.add_to_blocklist);
                        }
                        TextView menuSpamLabel = (TextView) menuView.findViewById(R.id.menuSpam);
                        if (menuSpamLabel != null) {
                            menuSpamLabel.setText(SpamUtil.isSpam(CallDetailActivity.this, CallDetailActivity.this.number) ? R.string.spam_menu_remove : R.string.spam_menu_mark);
                        }
                        final AlertDialog menuDlg = new AlertDialog.Builder(CallDetailActivity.this).setView(menuView).create();
                        if (menuDlg.getWindow() != null) {
                            try { menuDlg.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0)); } catch (Exception eBg) { }
                        }
                        menuView.findViewById(R.id.menuShareText).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    Intent share = new Intent("android.intent.action.SEND");
                                    share.setType("text/plain");
                                    share.putExtra("android.intent.extra.TEXT", (CallDetailActivity.this.contactName != null ? CallDetailActivity.this.contactName + ": " : "") + CallDetailActivity.this.number);
                                    CallDetailActivity.this.startActivity(Intent.createChooser(share, "Share"));
                                } catch (Exception e) {
                                }
                            }
                        });
                        menuView.findViewById(R.id.menuShareVcard).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    String vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:" + (CallDetailActivity.this.contactName != null ? CallDetailActivity.this.contactName : "") + "\nTEL;TYPE=CELL:" + CallDetailActivity.this.number + "\nEND:VCARD";
                                    Intent vc = new Intent("android.intent.action.SEND");
                                    vc.setType("text/x-vcard");
                                    vc.putExtra("android.intent.extra.TEXT", vcard);
                                    CallDetailActivity.this.startActivity(Intent.createChooser(vc, "Share"));
                                } catch (Exception e) {
                                }
                            }
                        });
                        menuView.findViewById(R.id.menuFav).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try { CallDetailActivity.this.findViewById(R.id.detailStarButton).performClick(); } catch (Exception e) { }
                            }
                        });
                        menuView.findViewById(R.id.menuShortcut).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                                        android.content.pm.ShortcutManager sm = (android.content.pm.ShortcutManager) CallDetailActivity.this.getSystemService(android.content.pm.ShortcutManager.class);
                                        if (sm != null && sm.isRequestPinShortcutSupported()) {
                                            android.content.pm.ShortcutInfo si = new android.content.pm.ShortcutInfo.Builder(CallDetailActivity.this, "dial_" + CallDetailActivity.this.number)
                                                    .setShortLabel(CallDetailActivity.this.contactName != null ? CallDetailActivity.this.contactName : CallDetailActivity.this.number)
                                                    .setIcon(android.graphics.drawable.Icon.createWithResource(CallDetailActivity.this, R.drawable.ic_launcher))
                                                    .setIntent(new Intent("android.intent.action.DIAL", Uri.parse("tel:" + CallDetailActivity.this.number)).setPackage(CallDetailActivity.this.getPackageName()))
                                                    .build();
                                            sm.requestPinShortcut(si, null);
                                        } else {
                                            android.widget.Toast.makeText(CallDetailActivity.this, "Shortcuts are not supported on this device", 0).show();
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        });
                        menuView.findViewById(R.id.menuSpeedDial).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    CharSequence[] opts = new CharSequence[8];
                                    for (int k = 0; k < 8; k++) {
                                        String cur = CallDetailActivity.this.getSharedPreferences("mydialer", 0).getString("speed" + (k + 2), null);
                                        opts[k] = "Key " + (k + 2) + ((cur != null && cur.contains("|")) ? "  (" + cur.substring(0, cur.indexOf(124)) + ")" : "");
                                    }
                                    new AlertDialog.Builder(CallDetailActivity.this).setTitle(R.string.add_to_speed_dial).setItems(opts, new DialogInterface.OnClickListener() {
                                        @Override // android.content.DialogInterface.OnClickListener
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            int key = i + 1 + 1;
                                            String nm = CallDetailActivity.this.contactName;
                                            if (nm == null) {
                                                String tg = Tags.get(CallDetailActivity.this, CallDetailActivity.this.number);
                                                nm = tg != null ? tg : CallDetailActivity.this.number;
                                            }
                                            CallDetailActivity.this.getSharedPreferences("mydialer", 0).edit().putString("speed" + key, nm + "|" + CallDetailActivity.this.number).commit();
                                            android.widget.Toast.makeText(CallDetailActivity.this, R.string.speed_dial_set, 0).show();
                                        }
                                    }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
                                } catch (Exception eSd) {
                                }
                            }
                        });
                        menuView.findViewById(R.id.menuBlocklist).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try { CallDetailActivity.this.findViewById(R.id.detailBlockButton).performClick(); } catch (Exception e) { }
                            }
                        });
                        android.view.View menuRecRow = menuView.findViewById(R.id.menuRec);
                        if (menuRecRow != null) {
                            final RecordingsUtil.RecEntry recCd = RecordingsUtil.firstRecording(CallDetailActivity.this, CallDetailActivity.this.number);
                            if (recCd != null) {
                                menuRecRow.setVisibility(0);
                                menuRecRow.setOnClickListener(new View.OnClickListener() {
                                    @Override // android.view.View.OnClickListener
                                    public void onClick(View v) {
                                        try { menuDlg.dismiss(); } catch (Exception e) { }
                                        RecordingsUtil.showPlayerSheet(CallDetailActivity.this, recCd);
                                    }
                                });
                            }
                        }
                        menuView.findViewById(R.id.menuSpam).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    if (SpamUtil.isSpam(CallDetailActivity.this, CallDetailActivity.this.number)) {
                                        SpamUtil.remove(CallDetailActivity.this, CallDetailActivity.this.number);
                                        android.widget.Toast.makeText(CallDetailActivity.this, R.string.spam_removed, 0).show();
                                    } else {
                                        CallDetailActivity.this.showSpamCategoryChooser();
                                    }
                                } catch (Exception es) {
                                }
                            }
                        });
                        menuView.findViewById(R.id.menuSeries).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                CallDetailActivity.this.showSeriesDialog();
                            }
                        });
                        menuView.findViewById(R.id.menuDelete).setOnClickListener(new View.OnClickListener() {
                            @Override // android.view.View.OnClickListener
                            public void onClick(View v) {
                                try { menuDlg.dismiss(); } catch (Exception e) { }
                                try {
                                    String num = CallDetailActivity.this.number;
                                    int del = CallDetailActivity.this.getContentResolver().delete(android.provider.CallLog.Calls.CONTENT_URI, android.provider.CallLog.Calls.NUMBER + " LIKE ? OR " + android.provider.CallLog.Calls.NUMBER + " LIKE ?", new String[]{"%" + num, "%" + CallDetailActivity.this.digits(num)});
                                    android.widget.Toast.makeText(CallDetailActivity.this, del + " call(s) deleted", 0).show();
                                    CallDetailActivity.this.loadHistory();
                                } catch (Exception ed) {
                                    android.widget.Toast.makeText(CallDetailActivity.this, "Unable to delete", 0).show();
                                }
                            }
                        });
                        menuDlg.show();
                    } catch (Exception eP) {
                    }
                }
            });
        } catch (Exception eMenu) {
        }
        try {
            try {
                ((TextView) findViewById(R.id.emojiValue)).setText(EmojiUtil.display(this, this.number));
                findViewById(R.id.rowFriendEmoji).setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        CallDetailActivity.this.showEmojiSheet();
                    }
                });
            } catch (Exception unusedEmojiRow) {
            }
            findViewById(R.id.detailViewAll).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    CallDetailActivity.this.showAllHistory = !CallDetailActivity.this.showAllHistory;
                    try {
                        ((TextView) view).setText(CallDetailActivity.this.showAllHistory ? "Show less" : "View all");
                    } catch (Exception eT) {
                    }
                    CallDetailActivity.this.loadHistory();
                }
            });
        } catch (Exception eVa) {
        }
        try {
            findViewById(R.id.detailAvatarBox).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        if (CallDetailActivity.this.contactId <= 0) {
                            android.widget.Toast.makeText(CallDetailActivity.this, "Save the contact first", 0).show();
                            return;
                        }
                        if (android.os.Build.VERSION.SDK_INT >= 23 && CallDetailActivity.this.checkSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
                            CallDetailActivity.this.requestPermissions(new String[]{"android.permission.WRITE_CONTACTS"}, 104);
                            return;
                        }
                        Intent pick = new Intent("android.intent.action.PICK");
                        pick.setType("image/*");
                        CallDetailActivity.this.startActivityForResult(pick, 301);
                    } catch (Exception eDp) {
                        android.widget.Toast.makeText(CallDetailActivity.this, "Unable to open the gallery", 0).show();
                    }
                }
            });
        } catch (Exception eDp2) {
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 301 && resultCode == -1 && data != null && data.getData() != null) {
                android.graphics.Bitmap bmp = null;
                try {
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()), null, o);
                    int scale = 1;
                    while (o.outWidth / (scale * 2) >= 256 && o.outHeight / (scale * 2) >= 256) {
                        scale *= 2;
                    }
                    android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
                    o2.inSampleSize = scale;
                    bmp = android.graphics.BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()), null, o2);
                } catch (Exception eDec) {
                }
                if (bmp != null) {
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, bos);
                    byte[] photoBytes = bos.toByteArray();
                    long raw = -1;
                    android.database.Cursor rc = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"_id"}, "contact_id=?", new String[]{String.valueOf(this.contactId)}, null);
                    if (rc != null) {
                        if (rc.moveToFirst()) {
                            raw = rc.getLong(0);
                        }
                        rc.close();
                    }
                    if (raw > 0) {
                        try {
                            getContentResolver().delete(ContactsContract.Data.CONTENT_URI, "raw_contact_id=? AND mimetype=?", new String[]{String.valueOf(raw), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE});
                        } catch (Exception eDel) {
                        }
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put("raw_contact_id", Long.valueOf(raw));
                        values.put("mimetype", ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                        values.put("data15", photoBytes);
                        values.put("is_super_primary", Integer.valueOf(1));
                        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
                        getPrefs().edit().putBoolean("contact_photos", true).commit();
                        android.widget.Toast.makeText(this, "Photo updated", 0).show();
                        bindHeader();
                    } else {
                        android.widget.Toast.makeText(this, "Photo could not be saved", 0).show();
                    }
                }
            }
        } catch (Exception eRes) {
        }
    }



    public void bindHeader() {
        Bitmap contactPhoto;
        String str = Tags.get(this, this.number);
        String str2 = this.contactName;
        if (str2 == null) {
            str2 = str != null ? str : getString(R.string.unknown_caller);
        }
        TextView textView = (TextView) findViewById(R.id.detailTitle);
        textView.setText(getString(R.string.contact_details));
        String emojiPrefix = EmojiUtil.display(this, this.number);
        ((TextView) findViewById(R.id.detailName)).setText(emojiPrefix.isEmpty() ? str2 : emojiPrefix + " " + str2);
        final boolean privacyOnNum = getSharedPreferences("mydialer", 0).getBoolean("hide_number", false);
        final TextView detailNumView = (TextView) findViewById(R.id.detailNumber);
        detailNumView.setText(privacyOnNum ? PrivacyUtil.mask(this, this.number) : this.number);
        android.widget.ImageView eyeToggleView = (android.widget.ImageView) findViewById(R.id.eyeToggle);
        eyeToggleView.setVisibility(privacyOnNum ? 0 : 8);
        eyeToggleView.setImageResource(R.drawable.ic_eye_off);
        eyeToggleView.setOnClickListener(new View.OnClickListener() {
            private boolean revealed = false;
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                revealed = !revealed;
                detailNumView.setText(revealed ? CallDetailActivity.this.number : PrivacyUtil.mask(CallDetailActivity.this, CallDetailActivity.this.number));
                ((android.widget.ImageView) view).setImageResource(revealed ? R.drawable.ic_eye : R.drawable.ic_eye_off);
            }
        });
        TextView textView2 = (TextView) findViewById(R.id.detailAvatar);
        if (this.contactId > 0 && getPrefs().getBoolean("contact_photos", false) && (contactPhoto = getContactPhoto(this.contactId)) != null) {
            textView2.setText("");
            try {
                textView2.setOutlineProvider(new ViewOutlineProvider() {
                    @Override // android.view.ViewOutlineProvider
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                textView2.setClipToOutline(true);
            } catch (Exception unused) {
            }
            textView2.setBackground(new BitmapDrawable(getResources(), contactPhoto));
            return;
        }
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(1);
        int[] iArr = AVATAR_COLORS;
        gradientDrawable.setColor(iArr[Math.abs(str2.toLowerCase(Locale.US).hashCode()) % iArr.length]);
        textView2.setBackground(gradientDrawable);
        textView2.setText(str2.substring(0, 1).toUpperCase(Locale.US));
    }

    private boolean canReadCalls() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CALL_LOG") == 0;
    }

    private boolean canReadContacts() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CONTACTS") == 0;
    }

    private void copyForPaste() {
        try {
            ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("phone", this.number));
            Toast.makeText(this, R.string.number_copied_paste, 0).show();
        } catch (Exception unused) {
        }
    }

    private static String digits(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Bitmap getContactPhoto(long j) {
        try {
            InputStream openContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, j), true);
            if (openContactPhotoInputStream == null) {
                return null;
            }
            Bitmap decodeStream = BitmapFactory.decodeStream(openContactPhotoInputStream);
            openContactPhotoInputStream.close();
            return decodeStream;
        } catch (Exception unused) {
            return null;
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("mydialer", 0);
    }

    private String international() {
        String digits = digits(this.number);
        if (digits.length() != 10) {
            return digits;
        }
        return "91" + digits;
    }

    private boolean isBlocked() {
        Set<String> stringSet = getPrefs().getStringSet("blocked", null);
        return stringSet != null && stringSet.contains(digits(this.number));
    }

    /* SIM slot from a CallLog phone_account_id */
    private int simSlotFor(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return -1;
        }
        try {
            android.telecom.TelecomManager tm = (android.telecom.TelecomManager) getSystemService("telecom");
            if (tm == null) {
                return -1;
            }
            java.util.List<android.telecom.PhoneAccountHandle> phs = tm.getCallCapablePhoneAccounts();
            if (phs == null) {
                return -1;
            }
            for (int idx = 0; idx < phs.size(); idx++) {
                if (accountId.equals(phs.get(idx).getId())) {
                    return idx;
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    /* Last known SIM slot for a number (captured live from our in-call screen) */
    private int simHintFor(String number) {
        try {
            String d = digits(number);
            if (d.length() >= 7) {
                java.util.Set<String> set = getSharedPreferences("mydialer", 0).getStringSet("sim_hint", null);
                if (set != null) {
                    for (String s : set) {
                        int i = s.indexOf('|');
                        if (i > 0 && s.substring(0, i).equals(d)) {
                            return Integer.parseInt(s.substring(i + 1));
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    private void loadHistory() {
        Cursor query;
        int i;
        String valueOf;
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.historyContainer);
        linearLayout.removeAllViews();
        if (canReadCalls()) {
            String digits = digits(this.number);
            try {
                query = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"number", "date", "type", "duration", "phone_account_id"}, null, null, "date DESC");
            } catch (Exception eCol) {
                query = null;
                try {
                    query = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"number", "date", "type", "duration"}, null, null, "date DESC");
                } catch (Exception eCol2) {
                    query = null;
                }
            }
            if (digits.isEmpty() || query == null) {
                return;
            }
            LayoutInflater layoutInflater = getLayoutInflater();
            boolean z = false;
            int i2 = 0;
            while (query.moveToNext() && i2 < (this.showAllHistory ? 50 : 3)) {
                String string = query.getString(z ? 1 : 0);
                if (string != null) {
                    String digits2 = digits(string);
                    boolean z2 = digits2.length() >= digits.length() && digits2.endsWith(digits);
                    if (!z2 && digits2.length() > 0) {
                        z2 = digits.endsWith(digits2);
                    }
                    if (z2) {
                        View inflate = layoutInflater.inflate(R.layout.item_detail_row, linearLayout, z);
                        int i3 = query.getInt(2);
                        long j = query.getLong(3);
                        i = i2;
                        TextView textView = (TextView) inflate.findViewById(R.id.rowType);
                        textView.setTextColor(getResources().getColor(R.color.text_main));
                        try {
                            textView.setText(new java.text.SimpleDateFormat("dd/MM/yy h:mm a", java.util.Locale.US).format(new java.util.Date(query.getLong(1))));
                        } catch (Exception eTime) {
                            textView.setText(String.valueOf(query.getLong(1)));
                        }
                        String label = typeLabel(i3);
                        if (j > 0 && i3 != 3) {
                            label = label + "  -  " + (j < 60 ? j + " s" : (j / 60) + ":" + (j % 60 < 10 ? "0" + (j % 60) : String.valueOf(j % 60)));
                        }
                        try {
                            if (query.getColumnCount() > 4) {
                                int slotCd = simSlotFor(query.getString(4));
                                if (slotCd < 0) {
                                    slotCd = simHintFor(string);
                                }
                                if (slotCd >= 0 && slotCd <= 1) {
                                    label = label + "  -  SIM " + (slotCd + 1);
                                }
                            }
                        } catch (Exception eSim) {
                        }
                        TextView infoText = (TextView) inflate.findViewById(R.id.rowInfo);
                        infoText.setText(label);
                        infoText.setTextColor(getResources().getColor(i3 == 3 ? R.color.missed_red : R.color.text_hint));
                        try {
                            ImageView arrowView = (ImageView) inflate.findViewById(R.id.rowArrow);
                            if (arrowView != null) {
                                arrowView.setImageResource(i3 == 2 ? R.drawable.ic_arrow_out : R.drawable.ic_arrow_in);
                                if (i3 == 3) {
                                    arrowView.setColorFilter(getResources().getColor(R.color.missed_red));
                                }
                            }
                        } catch (Exception eArrow) {
                        }
                        linearLayout.addView(inflate);
                        int i4 = i;
                        if (i4 < 49) {
                            View view = new View(this);
                            view.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
                            view.setBackgroundColor(536870911);
                            linearLayout.addView(view);
                        }
                        i2 = i4 + 1;
                        z = false;
                    }
                }
            }
            query.close();
            if (i2 == 0) {
                TextView textView2 = new TextView(this);
                textView2.setText(R.string.no_recents);
                textView2.setTextColor(getResources().getColor(R.color.text_hint));
                textView2.setTextSize(15.0f);
                textView2.setPadding(40, 40, 40, 40);
                linearLayout.addView(textView2);
            }
        }
    }

    private void lookupContact() {
        if (canReadContacts() && !this.number.isEmpty()) {
            try {
                Cursor query = getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(this.number)), new String[]{"display_name", "_id"}, null, null, null);
                if (query == null) {
                    return;
                }
                if (query.moveToFirst()) {
                    this.contactName = query.getString(0);
                    this.contactId = query.getLong(1);
                }
                query.close();
            } catch (Exception unused) {
            }
        }
    }


    public void openArattai() {
        try {
            for (ApplicationInfo applicationInfo : getPackageManager().getInstalledApplications(0)) {
                if (applicationInfo.packageName.toLowerCase().contains("arattai")) {
                    copyForPaste();
                    Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName);
                    if (launchIntentForPackage != null) {
                        startActivity(launchIntentForPackage);
                        return;
                    }
                }
            }
            Toast.makeText(this, "Arattai " + getString(R.string.not_installed), 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, "Arattai " + getString(R.string.not_installed), 0).show();
        }
    }


    public void openEmail() {
        Cursor query;
        try {
            if (this.contactId > 0 && (query = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, new String[]{"data1"}, "contact_id=?", new String[]{String.valueOf(this.contactId)}, null)) != null) {
                String string = query.moveToFirst() ? query.getString(0) : null;
                query.close();
                if (string != null && !string.isEmpty()) {
                    startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("mailto:" + Uri.encode(string))));
                    return;
                }
            }
            Toast.makeText(this, R.string.no_email, 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, R.string.no_email, 0).show();
        }
    }


    public void openTelegram() {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("tg://resolve?phone=" + international()));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage("org.telegram.messenger");
            if (launchIntentForPackage != null) {
                copyForPaste();
                startActivity(launchIntentForPackage);
            } else {
                Toast.makeText(this, "Telegram " + getString(R.string.not_installed), 0).show();
            }
        } catch (Exception unused) {
            Toast.makeText(this, "Telegram " + getString(R.string.not_installed), 0).show();
        }
    }


    public void openWhatsApp() {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + international())));
        } catch (Exception unused) {
            Toast.makeText(this, "WhatsApp " + getString(R.string.not_installed), 0).show();
        }
    }

    public void openWhatsAppBusiness() {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + international()));
            intent.setPackage("com.whatsapp.w4b");
            startActivity(intent);
        } catch (Exception unused) {
            try {
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + international())));
            } catch (Exception unused2) {
                Toast.makeText(this, "WhatsApp Business " + getString(R.string.not_installed), 0).show();
            }
        }
    }


    public void showTagDialog() {
        final EditText editText = new EditText(this);
        String str = Tags.get(this, this.number);
        if (str != null) {
            editText.setText(str);
        }
        new AlertDialog.Builder(this).setTitle(R.string.add_tag).setMessage(R.string.add_tag_msg).setView(editText).setPositiveButton(R.string.save_tag, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                String trim = editText.getText().toString().trim();
                CallDetailActivity callDetailActivity = CallDetailActivity.this;
                Tags.set(callDetailActivity, callDetailActivity.number, trim);
                Toast.makeText(CallDetailActivity.this, trim.isEmpty() ? R.string.tag_removed : R.string.tag_saved, 0).show();
                CallDetailActivity.this.bindHeader();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }


    public void toggleBlock() {
        Set<String> stringSet = getPrefs().getStringSet("blocked", null);
        HashSet hashSet = stringSet == null ? new HashSet() : new HashSet(stringSet);
        if (this.blocked) {
            hashSet.remove(digits(this.number));
        } else {
            hashSet.add(digits(this.number));
        }
        getPrefs().edit().putStringSet("blocked", hashSet).commit();
        this.blocked = !this.blocked;
        updateBlockUi();
        Toast.makeText(this, this.blocked ? R.string.blocked_toast : R.string.unblocked_toast, 0).show();
        try {
            String nm = this.contactName;
            if (nm == null) {
                String tg = Tags.get(this, this.number);
                nm = tg != null ? tg : getString(R.string.unknown_caller);
            }
            String em = EmojiUtil.display(this, this.number);
            ((TextView) findViewById(R.id.detailName)).setText(em.isEmpty() ? nm : em + " " + nm);
            ((TextView) findViewById(R.id.emojiValue)).setText(em);
        } catch (Exception unusedRefresh) {
        }
    }

    private String typeLabel(int i) {
        return i == 1 ? getString(R.string.type_incoming) : i == 2 ? getString(R.string.type_outgoing) : i == 3 ? getString(R.string.type_missed) : i == 5 ? getString(R.string.type_rejected) : getString(R.string.type_other);
    }

    private android.app.AlertDialog emojiSheet;

    /* Bottom sheet with the 7 preset friend emojis */
    private void showEmojiSheet() {
        android.widget.LinearLayout sheet = new android.widget.LinearLayout(this);
        sheet.setOrientation(android.widget.LinearLayout.VERTICAL);
        sheet.setPadding(28, 24, 28, 32);
        sheet.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_settings_box));
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(getString(R.string.friend_emoji));
        title.setTextSize(16.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        sheet.addView(title);
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        row.setPadding(0, 20, 0, 8);
        for (int i = 0; i < EmojiUtil.PRESETS.length; i++) {
            final int idx = i;
            android.widget.TextView em = new android.widget.TextView(this);
            em.setText(EmojiUtil.PRESETS[i]);
            em.setTextSize(26.0f);
            em.setPadding(16, 12, 16, 12);
            em.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    EmojiUtil.set(CallDetailActivity.this, CallDetailActivity.this.number, EmojiUtil.PRESETS[idx]);
                    ((TextView) CallDetailActivity.this.findViewById(R.id.emojiValue)).setText(EmojiUtil.display(CallDetailActivity.this, CallDetailActivity.this.number));
                    try {
                        CallDetailActivity.this.emojiSheet.dismiss();
                    } catch (Exception unusedDismiss) {
                    }
                }
            });
            row.addView(em);
        }
        sheet.addView(row);
        android.widget.TextView remove = new android.widget.TextView(this);
        remove.setText(getString(R.string.emoji_remove));
        remove.setTextSize(14.0f);
        remove.setGravity(android.view.Gravity.CENTER);
        remove.setTextColor(getResources().getColor(R.color.text_hint));
        remove.setPadding(0, 16, 0, 4);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                EmojiUtil.set(CallDetailActivity.this, CallDetailActivity.this.number, "");
                ((TextView) CallDetailActivity.this.findViewById(R.id.emojiValue)).setText("");
                try {
                    CallDetailActivity.this.emojiSheet.dismiss();
                } catch (Exception unusedDismiss) {
                }
            }
        });
        sheet.addView(remove);
        this.emojiSheet = new AlertDialog.Builder(this).setView(sheet).create();
        this.emojiSheet.show();
        try {
            android.view.Window w = this.emojiSheet.getWindow();
            if (w != null) {
                w.setGravity(android.view.Gravity.BOTTOM);
                w.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            }
        } catch (Exception unusedWindow) {
        }
    }

    private void updateBlockUi() {
        try {
            ((TextView) findViewById(R.id.detailBlockLabel)).setText(this.blocked ? R.string.unblock_number : R.string.block_number);
        } catch (Exception unused) {
        }
    }

    private int themeGen;
    private boolean showAllHistory;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_call_detail);
        String stringExtra = getIntent().getStringExtra("number");
        this.number = stringExtra;
        this.phoneAccount = getIntent().getStringExtra("phone_account");
        if (stringExtra == null) {
            this.number = "";
        }
        findViewById(R.id.detailBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallDetailActivity.this.finish();
            }
        });
        lookupContact();
        bindHeader();
        bindActions();
        loadHistory();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
        lookupContact();
        bindHeader();
        this.blocked = isBlocked();
        updateBlockUi();
        loadHistory();
    }
}
