package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
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
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final int REQ_PERMS = 100;
    private static final int REQ_PICK_SPEED = 400;
    private static final int REQ_SETTINGS = 300;
    private static final int REQ_VOICE = 500;
    private ImageButton addContactButton;
    private final List<ContactEntry> allContacts;
    private final ContactAdapter contactsAdapter;
    private TextView contactsEmpty;
    private ListView contactsList;
    private View contactsView;
    private FrameLayout content;
    private TextView dialpadMatch;
    private final List<ContactEntry> dialpadSuggestions;
    private View dialpadView;
    private final ContactAdapter favouritesAdapter;
    private TextView favouritesEmpty;
    private ListView favouritesList;
    private View favouritesView;
    private final List<ContactEntry> filteredContacts;
    private final List<ContactEntry> filteredFavourites;
    private TextView headerTitle;
    private int messagesFilter;
    private View messagesView;
    private TextView numberText;
    private final RecentsAdapter recentsAdapter;
    private TextView recentsEmpty;
    private ListView recentsList;
    private View recentsView;
    private EditText searchInput;
    private TextView simBadge;
    private final ContactAdapter suggestionsAdapter;
    private TextView tabContacts;
    private TextView tabFavourites;
    private TextView tabMessages;
    private TextView tabRecents;
    private ToneGenerator toneGenerator;
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    private static final String[] LETTERS = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};
    private static final int[] AVATAR_COLORS = {-8812853, -11684180, -6982195, -1023342, -10177034, -18611, -8271996, -749647, -6190977, -11677471};
    private static final String[] T9 = {"0", "1", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
    private static boolean unlockedThisSession = false;
    private String matchedName = null;
    private String matchedNumber = null;
    private int currentTab = 1;
    private int filterMode = 0;
    private int pendingSpeedSlot = -1;
    private final List<CallEntry> callEntries = new ArrayList();
    private final List<Object> recentsRows = new ArrayList();
    private final java.util.HashSet<String> expandedGroups = new java.util.HashSet<>();
    private android.database.ContentObserver callLogObserver;
    private final android.os.Handler callLogHandler = new android.os.Handler();
    private final List<CallEntry> filteredCalls = new ArrayList();
    private final HashMap<Long, String> accountByContact = new HashMap<>();
    private Set<String> allowedAccounts = null;
    private final List<ContactEntry> favouriteContacts = new ArrayList();


    /* loaded from: classes.dex */
    public static class CallEntry {
        final long date;
        final long duration;
        final String name;
        final String number;
        final String phoneAccount;
        final int type;

        CallEntry(String str, String str2, long j, int i, long j2, String phoneAccount) {
            this.name = str;
            this.number = str2;
            this.date = j;
            this.type = i;
            this.duration = j2;
            this.phoneAccount = phoneAccount;
        }
    }

    /* One grouped row in the Recents list: all calls from one number */
    public static class RecentsGroup {
        final CallEntry latest;
        final List<CallEntry> calls = new ArrayList();

        RecentsGroup(CallEntry latest) {
            this.latest = latest;
        }
    }


    /* loaded from: classes.dex */
    public class ContactAdapter extends BaseAdapter {
        private final List<ContactEntry> data;
        private final boolean openDetailOnTap;

        ContactAdapter(MainActivity mainActivity, List<ContactEntry> list) {
            this(list, false);
        }

        ContactAdapter(List<ContactEntry> list, boolean z) {
            this.data = list;
            this.openDetailOnTap = z;
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return this.data.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return this.data.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.item_contact, viewGroup, false);
            }
            final ContactEntry contactEntry = this.data.get(i);
            MainActivity.this.setAvatar((TextView) view.findViewById(R.id.contactInitial), contactEntry.name, contactEntry.contactId);
            String conEmoji = EmojiUtil.display(MainActivity.this, contactEntry.number);
            ((TextView) view.findViewById(R.id.contactName)).setText(conEmoji.isEmpty() ? contactEntry.name : conEmoji + " " + contactEntry.name);
            ((TextView) view.findViewById(R.id.contactNumber)).setText(PrivacyUtil.mask(MainActivity.this, contactEntry.number));
            view.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    if (contactEntry.contactId == -2) {
                        MainActivity.this.openOwnProfile();
                        return;
                    }
                    if (!ContactAdapter.this.openDetailOnTap || !"details".equals(MainActivity.this.getPrefs().getString("contact_click", "call"))) {
                        MainActivity.this.callNumber(contactEntry.number);
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, (Class<?>) CallDetailActivity.class);
                    intent.putExtra("number", contactEntry.number);
                    MainActivity.this.startActivity(intent);
                }
            });
            view.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0.0f;
                private float startY = 0.0f;

                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view2, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == 0) {
                        this.startX = motionEvent.getX();
                        this.startY = motionEvent.getY();
                        return false;
                    }
                    if (motionEvent.getAction() != 1) {
                        return false;
                    }
                    float x = motionEvent.getX() - this.startX;
                    float y = motionEvent.getY() - this.startY;
                    if (Math.abs(x) <= 150.0f || Math.abs(x) <= Math.abs(y) * 2.0f) {
                        return false;
                    }
                    if (x > 0.0f) {
                        MainActivity.this.callNumber(contactEntry.number);
                    } else {
                        MainActivity.this.sendSms(contactEntry.number);
                    }
                    return true;
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view2) {
                    MainActivity.this.showNumberOptions(contactEntry.name, contactEntry.number);
                    return true;
                }
            });
            return view;
        }
    }


    /* loaded from: classes.dex */
    public static class ContactEntry {
        final long contactId;
        final String name;
        final String number;

        ContactEntry(String str, String str2, long j) {
            this.name = str;
            this.number = str2;
            this.contactId = j;
        }
    }


    /* loaded from: classes.dex */
    public class RecentsAdapter extends BaseAdapter {
        private RecentsAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return MainActivity.this.recentsRows.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return MainActivity.this.recentsRows.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public int getItemViewType(int i) {
            Object row = MainActivity.this.recentsRows.get(i);
            if (row instanceof String) {
                return 0;
            }
            return row instanceof RecentsGroup ? 1 : 2;
        }

        @Override // android.widget.Adapter
        public int getViewTypeCount() {
            return 3;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            Object row = MainActivity.this.recentsRows.get(i);
            if (row instanceof String) {
                TextView header;
                if (view == null) {
                    header = new TextView(MainActivity.this);
                    header.setTextSize(12.0f);
                    header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    header.setTextColor(MainActivity.this.getResources().getColor(R.color.dialer_blue));
                    header.setPadding(M3Util.dp(MainActivity.this, 20), M3Util.dp(MainActivity.this, 16), M3Util.dp(MainActivity.this, 16), M3Util.dp(MainActivity.this, 4));
                } else {
                    header = (TextView) view;
                }
                header.setText((String) row);
                return header;
            }
            if (row instanceof RecentsGroup) {
                final RecentsGroup g = (RecentsGroup) row;
                if (view == null) {
                    view = MainActivity.this.getLayoutInflater().inflate(R.layout.item_call, viewGroup, false);
                }
                bindGroupRow(view, g);
                return view;
            }
            final CallEntry ce = (CallEntry) row;
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.item_call, viewGroup, false);
                view.setPadding(M3Util.dp(MainActivity.this, 52), view.getPaddingTop(), M3Util.dp(MainActivity.this, 16), view.getPaddingBottom());
            }
            bindSubRow(view, ce);
            return view;
        }

        private void setSimBadge(View view, String phoneAccount, String number) {
            TextView simBadge = (TextView) view.findViewById(R.id.simBadge);
            if (simBadge == null) {
                return;
            }
            int slot = MainActivity.this.simSlotFor(phoneAccount);
            if (slot < 0) {
                slot = MainActivity.this.simHintFor(number);
            }
            if (slot < 0 || slot > 1) {
                simBadge.setVisibility(8);
                return;
            }
            simBadge.setVisibility(0);
            simBadge.setText(String.valueOf(slot + 1));
            android.graphics.drawable.GradientDrawable sd = new android.graphics.drawable.GradientDrawable();
            sd.setCornerRadius(M3Util.dp(MainActivity.this, 6));
            sd.setColor(slot == 0 ? 0xff2fbf4f : 0xff2196f3);
            simBadge.setBackgroundDrawable(sd);
            simBadge.setTextColor(0xffffffff);
        }

        private void bindGroupRow(View view, final RecentsGroup g) {
            CallEntry callEntry = g.latest;
            View deleteOverlay = view.findViewById(R.id.deleteOverlay);
            if (deleteOverlay != null) {
                deleteOverlay.setVisibility(8);
            }
            TextView avatarTv = (TextView) view.findViewById(R.id.callAvatar);
            TextView nameTv = (TextView) view.findViewById(R.id.callerName);
            TextView detailsTv = (TextView) view.findViewById(R.id.callDetails);
            nameTv.setTextSize(16.0f);
            detailsTv.setTextSize(13.0f);
            ContactEntry findContactByNumber = MainActivity.this.findContactByNumber(callEntry.number);
            if (findContactByNumber != null) {
                MainActivity.this.setAvatar(avatarTv, findContactByNumber.name, findContactByNumber.contactId);
            } else {
                MainActivity.this.setAvatar(avatarTv, callEntry.name, -1L);
            }
            String str2 = Tags.get(MainActivity.this, callEntry.number);
            String recEmoji = EmojiUtil.display(MainActivity.this, callEntry.number);
            String recName;
            if (callEntry.name != null && !callEntry.name.trim().isEmpty()) {
                recName = callEntry.name;
            } else if (findContactByNumber != null && findContactByNumber.name != null && !findContactByNumber.name.trim().isEmpty()) {
                recName = findContactByNumber.name;
            } else if (str2 != null && !str2.trim().isEmpty()) {
                recName = str2;
            } else {
                recName = MainActivity.this.getString(R.string.unknown_caller);
            }
            int cnt = g.calls.size();
            String cntLabel = cnt > 1 ? " (" + (cnt > 5 ? "5+" : String.valueOf(cnt)) + ")" : "";
            nameTv.setText((recEmoji.isEmpty() ? recName : recEmoji + " " + recName) + cntLabel);
            boolean z = false;
            if (str2 != null && str2.toLowerCase(Locale.US).contains("spam")) {
                z = true;
            }
            if (SpamUtil.isSpam(MainActivity.this, callEntry.number)) {
                z = true;
            }
            nameTv.setTextColor(MainActivity.this.getResources().getColor((callEntry.type == 3 || z) ? R.color.missed_red : R.color.text_main));
            CharSequence rel = DateUtils.getRelativeTimeSpanString(MainActivity.this, callEntry.date);
            String dur = callEntry.duration > 0 ? "  |  " + MainActivity.this.formatDuration(callEntry.duration) : "";
            detailsTv.setText(PrivacyUtil.mask(MainActivity.this, callEntry.number) + "  |  " + MainActivity.this.typeLabel(callEntry.type) + MainActivity.this.numberTypeLabel(callEntry.number) + "  |  " + ((Object) rel) + dur);
            TextView spamBadge = (TextView) view.findViewById(R.id.spamBadge);
            if (spamBadge != null) {
                String catR = SpamUtil.getCategory(MainActivity.this, callEntry.number);
                if (catR == null) {
                    String susR = SpamUtil.getSuspected(MainActivity.this, callEntry.number);
                    if (susR != null) {
                        spamBadge.setVisibility(0);
                        spamBadge.setText(susR);
                        spamBadge.setBackgroundResource(R.drawable.bg_suspect_badge);
                    } else {
                        spamBadge.setVisibility(8);
                    }
                } else {
                    spamBadge.setVisibility(0);
                    spamBadge.setText(catR);
                    spamBadge.setBackgroundResource(R.drawable.bg_spam_badge);
                }
            }
            setSimBadge(view, callEntry.phoneAccount, callEntry.number);
            android.widget.ImageView recBtn = (android.widget.ImageView) view.findViewById(R.id.recBtn);
            if (recBtn != null) {
                final RecordingsUtil.RecEntry rec = RecordingsUtil.firstRecording(MainActivity.this, callEntry.number);
                if (rec != null) {
                    recBtn.setVisibility(0);
                    recBtn.setOnClickListener(new View.OnClickListener() {
                        @Override // android.view.View.OnClickListener
                        public void onClick(View view2) {
                            RecordingsUtil.showPlayerSheet(MainActivity.this, rec);
                        }
                    });
                } else {
                    recBtn.setVisibility(8);
                }
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    Intent intent = new Intent(MainActivity.this, (Class<?>) CallDetailActivity.class);
                    intent.putExtra("number", g.latest.number);
                    intent.putExtra("phone_account", g.latest.phoneAccount);
                    MainActivity.this.startActivity(intent);
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view2) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(g.latest.name != null ? g.latest.name : PrivacyUtil.mask(MainActivity.this, g.latest.number)).setItems(new CharSequence[]{MainActivity.this.getString(R.string.delete_from_history)}, new DialogInterface.OnClickListener() {
                        @Override // android.content.DialogInterface.OnClickListener
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                for (CallEntry del : new ArrayList<>(g.calls)) {
                                    MainActivity.this.deleteCallEntry(del);
                                }
                                MainActivity.this.loadCalls();
                                MainActivity.this.applyCurrentFilter();
                            }
                        }
                    }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
                    return true;
                }
            });
        }

        private void bindSubRow(View view, final CallEntry callEntry) {
            View deleteOverlay = view.findViewById(R.id.deleteOverlay);
            if (deleteOverlay != null) {
                deleteOverlay.setVisibility(8);
                deleteOverlay.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view2) {
                        MainActivity.this.deleteCallEntry(callEntry);
                    }
                });
            }
            TextView avatarTv = (TextView) view.findViewById(R.id.callAvatar);
            TextView nameTv = (TextView) view.findViewById(R.id.callerName);
            TextView detailsTv = (TextView) view.findViewById(R.id.callDetails);
            nameTv.setTextSize(14.0f);
            detailsTv.setTextSize(12.0f);
            ContactEntry findContactByNumber = MainActivity.this.findContactByNumber(callEntry.number);
            if (findContactByNumber != null) {
                MainActivity.this.setAvatar(avatarTv, findContactByNumber.name, findContactByNumber.contactId);
            } else {
                MainActivity.this.setAvatar(avatarTv, callEntry.name, -1L);
            }
            String dur = callEntry.duration > 0 ? "  |  " + MainActivity.this.formatDuration(callEntry.duration) : "";
            nameTv.setText(MainActivity.this.typeLabel(callEntry.type) + "  ·  " + MainActivity.this.timeHM(callEntry.date) + (callEntry.type == 3 ? "" : dur));
            nameTv.setTextColor(MainActivity.this.getResources().getColor(callEntry.type == 3 ? R.color.missed_red : R.color.text_hint));
            detailsTv.setText(PrivacyUtil.mask(MainActivity.this, callEntry.number));
            TextView spamBadge = (TextView) view.findViewById(R.id.spamBadge);
            if (spamBadge != null) {
                spamBadge.setVisibility(8);
            }
            setSimBadge(view, callEntry.phoneAccount, callEntry.number);
            android.widget.ImageView recBtnSub = (android.widget.ImageView) view.findViewById(R.id.recBtn);
            if (recBtnSub != null) {
                recBtnSub.setVisibility(8);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    Intent intent = new Intent(MainActivity.this, (Class<?>) CallDetailActivity.class);
                    intent.putExtra("number", callEntry.number);
                    intent.putExtra("phone_account", callEntry.phoneAccount);
                    MainActivity.this.startActivity(intent);
                }
            });
            view.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0.0f;
                private float startY = 0.0f;

                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view2, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == 0) {
                        this.startX = motionEvent.getX();
                        this.startY = motionEvent.getY();
                        return false;
                    }
                    if (motionEvent.getAction() != 1) {
                        return false;
                    }
                    float x = motionEvent.getX() - this.startX;
                    float y = motionEvent.getY() - this.startY;
                    if (Math.abs(x) <= 150.0f || Math.abs(x) <= Math.abs(y) * 2.0f) {
                        return false;
                    }
                    View deleteOv = view2.findViewById(R.id.deleteOverlay);
                    if (deleteOv != null) {
                        deleteOv.setVisibility(0);
                    }
                    return true;
                }
            });
        }
    }

    /* loaded from: classes.dex */
    private class ThreadsAdapter extends BaseAdapter {
        private final ArrayList<Object[]> data;
        private final HashMap<String, String> nameCache = new HashMap<>();

        ThreadsAdapter(ArrayList<Object[]> arrayList) {
            this.data = arrayList;
        }

        private String lookupName(String str) {
            if (str == null) {
                return null;
            }
            if (this.nameCache.containsKey(str)) {
                return this.nameCache.get(str);
            }
            String r0 = null;
            try {
                Cursor query = MainActivity.this.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"display_name"}, null, null, null);
                if (query != null) {
                    r0 = query.moveToFirst() ? query.getString(0) : null;
                    query.close();
                }
            } catch (Exception unused) {
            }
            this.nameCache.put(str, r0);
            return r0;
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return this.data.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return this.data.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return ((Long) this.data.get(i)[0]).longValue();
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.item_thread, viewGroup, false);
            }
            Object[] objArr = this.data.get(i);
            String str = (String) objArr[1];
            String str2 = (String) objArr[2];
            long longValue = ((Long) objArr[3]).longValue();
            boolean equals = Boolean.TRUE.equals(objArr[4]);
            TextView textView = (TextView) view.findViewById(R.id.threadAvatar);
            TextView textView2 = (TextView) view.findViewById(R.id.threadName);
            TextView textView3 = (TextView) view.findViewById(R.id.threadSnippet);
            TextView textView4 = (TextView) view.findViewById(R.id.threadTime);
            TextView textView5 = (TextView) view.findViewById(R.id.threadBadge);
            if (textView5 != null) {
                int intValue = ((Integer) objArr[5]).intValue();
                if (intValue > 0) {
                    textView5.setVisibility(0);
                    textView5.setText(String.valueOf(intValue));
                } else {
                    textView5.setVisibility(8);
                }
            }
            String threadEmoji = EmojiUtil.display(MainActivity.this, str);
            String lookupName = lookupName(str);
            if (lookupName != null) {
                str = lookupName;
            } else {
                str = PrivacyUtil.mask(MainActivity.this, str);
                if (str == null || str.isEmpty()) {
                    str = "?";
                }
            }
            textView2.setText(threadEmoji.isEmpty() ? str : threadEmoji + " " + str);
            if (str2 == null) {
                str2 = "";
            }
            textView3.setText(str2);
            textView3.setTextColor(MainActivity.this.getResources().getColor(equals ? R.color.text_main : R.color.text_hint));
            textView4.setText(DateUtils.getRelativeTimeSpanString(longValue));
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            gradientDrawable.setColor(MainActivity.AVATAR_COLORS[Math.abs(str.toLowerCase(Locale.US).hashCode()) % MainActivity.AVATAR_COLORS.length]);
            textView.setBackground(gradientDrawable);
            textView.setText(str.substring(0, 1).toUpperCase(Locale.US));
            return view;
        }
    }

    public MainActivity() {
        ArrayList arrayList = new ArrayList();
        this.filteredFavourites = arrayList;
        this.allContacts = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        this.filteredContacts = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        this.dialpadSuggestions = arrayList3;
        this.recentsAdapter = new RecentsAdapter();
        this.favouritesAdapter = new ContactAdapter(this, arrayList);
        this.contactsAdapter = new ContactAdapter(arrayList2, true);
        this.suggestionsAdapter = new ContactAdapter(this, arrayList3);
    }

    /* v141: key by account TYPE only - matches SettingsActivity dedup */
    private static String accountKey(String str, String str2) {
        return str == null ? "" : str;
    }


    public void addToContacts(String str) {
        try {
            Intent intent = new Intent("android.intent.action.INSERT");
            intent.setType("vnd.android.cursor.dir/raw_contact");
            intent.putExtra("phone", str);
            startActivity(intent);
        } catch (Exception unused) {
            Toast.makeText(this, "Could not open contacts", 0).show();
        }
    }

    /* Last known SIM slot for a number, captured live from our in-call screen */
    public int simHintFor(String number) {
        try {
            String d = digitsOnly(number);
            if (d.length() >= 7) {
                java.util.Set<String> set = getPrefs().getStringSet("sim_hint", null);
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

    /* SIM slot (0 = SIM 1, 1 = SIM 2, -1 unknown) for a CallLog phone_account_id */
    public int simSlotFor(String phoneAccountId) {
        if (phoneAccountId == null || phoneAccountId.isEmpty()) {
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
                if (phoneAccountId.equals(phs.get(idx).getId())) {
                    return idx;
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    /* Time of day label like 7:28 pm */
    public String timeHM(long date) {
        try {
            return new java.text.SimpleDateFormat("h:mm a", Locale.US).format(new java.util.Date(date));
        } catch (Exception e) {
            return "";
        }
    }

    /* Section header label: Today / Yesterday / d MMM yyyy */
    public String dateHeaderFor(long date) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar then = java.util.Calendar.getInstance();
        then.setTimeInMillis(date);
        if (now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) && now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)) {
            return getString(R.string.today_h);
        }
        now.add(java.util.Calendar.DAY_OF_YEAR, -1);
        if (now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) && now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)) {
            return getString(R.string.yesterday_h);
        }
        try {
            return new java.text.SimpleDateFormat("d MMM yyyy", Locale.US).format(new java.util.Date(date));
        } catch (Exception e) {
            return "";
        }
    }

    /* Build the flat row list: date headers + grouped calls + expanded sub-rows */
    public void buildRecentsRows() {
        this.recentsRows.clear();
        java.util.LinkedHashMap<String, RecentsGroup> gm = new java.util.LinkedHashMap<>();
        for (CallEntry ce : this.filteredCalls) {
            String key = digitsOnly(ce.number);
            if (key.isEmpty()) {
                key = ce.number;
            }
            RecentsGroup g = gm.get(key);
            if (g == null) {
                g = new RecentsGroup(ce);
                gm.put(key, g);
            }
            g.calls.add(ce);
        }
        String lastHeader = null;
        for (RecentsGroup g : gm.values()) {
            String h = dateHeaderFor(g.latest.date);
            if (!h.equals(lastHeader)) {
                this.recentsRows.add(h);
                lastHeader = h;
            }
            this.recentsRows.add(g);
        }
    }


    public void applyCurrentFilter() {
        String lowerCase = this.searchInput.getText().toString().trim().toLowerCase(Locale.US);
        this.filteredCalls.clear();
        for (CallEntry callEntry : this.callEntries) {
            if (this.filterMode != 1 || callEntry.type == 3) {
                if (this.filterMode != 2 || callEntry.type == 1) {
                    if (this.filterMode != 3 || callEntry.type == 2) {
                        if (matches(lowerCase, callEntry.name, callEntry.number)) {
                            this.filteredCalls.add(callEntry);
                        }
                    }
                }
            }
        }
        Collections.sort(this.filteredCalls, new Comparator<CallEntry>() {
            @Override // java.util.Comparator
            public int compare(CallEntry callEntry2, CallEntry callEntry3) {
                if (callEntry3.date > callEntry2.date) {
                    return 1;
                }
                return callEntry3.date < callEntry2.date ? -1 : 0;
            }
        });
        this.filteredContacts.clear();
        for (ContactEntry contactEntry : this.allContacts) {
            if (matches(lowerCase, contactEntry.name, contactEntry.number)) {
                this.filteredContacts.add(contactEntry);
            }
        }
        this.filteredFavourites.clear();
        for (ContactEntry contactEntry2 : this.favouriteContacts) {
            if (matches(lowerCase, contactEntry2.name, contactEntry2.number)) {
                this.filteredFavourites.add(contactEntry2);
            }
        }
        buildRecentsRows();
        this.recentsAdapter.notifyDataSetChanged();
        this.contactsAdapter.notifyDataSetChanged();
        this.favouritesAdapter.notifyDataSetChanged();
        this.recentsEmpty.setVisibility(this.filteredCalls.isEmpty() ? 0 : 8);
        this.contactsEmpty.setVisibility(this.filteredContacts.isEmpty() ? 0 : 8);
        this.favouritesEmpty.setVisibility(this.filteredFavourites.isEmpty() ? 0 : 8);
    }

    private void applyDialpadBackground() {
        try {
            File file = new File(getFilesDir(), "dialer_bg.jpg");
            if (!file.exists()) {
                this.dialpadView.setBackgroundResource(0);
                return;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int i = 1;
            while (options.outWidth / i > 1080) {
                i *= 2;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i;
            Bitmap decodeFile = BitmapFactory.decodeFile(file.getAbsolutePath(), options2);
            if (decodeFile == null) {
                this.dialpadView.setBackgroundResource(0);
                return;
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), decodeFile);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(-1342177280);
            this.dialpadView.setBackground(new LayerDrawable(new Drawable[]{bitmapDrawable, gradientDrawable}));
        } catch (Exception unused) {
        }
    }


    public void blockNumber(String str) {
        Set<String> blockedNumbers = getBlockedNumbers();
        blockedNumbers.add(digitsOnly(str));
        setBlockedNumbers(blockedNumbers);
        Toast.makeText(this, R.string.blocked_toast, 0).show();
    }

    private void buildKeypad() {
        GridLayout gridLayout = (GridLayout) this.dialpadView.findViewById(R.id.keypadGrid);
        gridLayout.removeAllViews();
        LayoutInflater layoutInflater = getLayoutInflater();
        int i = 0;
        while (true) {
            String[] strArr = KEYS;
            if (i >= strArr.length) {
                return;
            }
            View inflate = layoutInflater.inflate(R.layout.item_key, (ViewGroup) gridLayout, false);
            TextView textView = (TextView) inflate.findViewById(R.id.digitText);
            TextView textView2 = (TextView) inflate.findViewById(R.id.lettersText);
            final String str = strArr[i];
            textView.setText(str);
            textView2.setText(LETTERS[i]);
            inflate.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.numberText.append(str);
                    MainActivity.this.keyFeedback(str);
                }
            });
            inflate.setOnLongClickListener(new View.OnLongClickListener() {
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view) {
                    return MainActivity.this.handleKeyLongPress(str);
                }
            });
            gridLayout.addView(inflate);
            i++;
        }
    }


    public void callNumber(final String str) {
        if (str == null || str.isEmpty()) {
            return;
        }
        if (!getPrefs().getBoolean("confirm_call", false)) {
            placeCall(str);
        } else {
            showCallConfirm(str);
        }
    }

    /* M3 call confirmation card */
    private void showCallConfirm(final String str) {
        try {
            ContactEntry findContactByNumber = findContactByNumber(str);
            String nm = findContactByNumber != null ? findContactByNumber.name : Tags.get(this, str);
            if (nm == null) {
                nm = PrivacyUtil.mask(this, str);
            }
            String em = EmojiUtil.display(this, str);
            android.widget.LinearLayout card = M3Util.card(this);
            card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            android.widget.TextView avatar = new android.widget.TextView(this);
            avatar.setText(nm.substring(0, 1).toUpperCase(Locale.US));
            avatar.setTextColor(-1);
            avatar.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(AVATAR_COLORS[Math.abs(nm.toLowerCase(Locale.US).hashCode()) % AVATAR_COLORS.length]);
            avatar.setBackgroundDrawable(g);
            card.addView(avatar, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 56), M3Util.dp(this, 56)));
            android.widget.TextView nameTv = new android.widget.TextView(this);
            nameTv.setText(em.isEmpty() ? nm : em + " " + nm);
            nameTv.setTextSize(20.0f);
            nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            nameTv.setTextColor(getResources().getColor(R.color.text_main));
            nameTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            nlp.topMargin = M3Util.dp(this, 12);
            card.addView(nameTv, nlp);
            android.widget.TextView numTv = new android.widget.TextView(this);
            numTv.setText(PrivacyUtil.mask(this, str));
            numTv.setTextSize(14.0f);
            numTv.setTextColor(getResources().getColor(R.color.text_hint));
            numTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams ulp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            ulp.topMargin = M3Util.dp(this, 2);
            card.addView(numTv, ulp);
            android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
            btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = M3Util.dp(this, 20);
            android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
            btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView callBtn = M3Util.greenBtn(this, getString(R.string.call_action), R.drawable.ic_call_white);
            android.widget.LinearLayout.LayoutParams clp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            clp.leftMargin = M3Util.dp(this, 10);
            btnRow.addView(callBtn, clp);
            card.addView(btnRow, blp);
            final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                }
            });
            callBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                    MainActivity.this.placeCall(str);
                }
            });
            M3Util.scrim(dlg);
        } catch (Exception e) {
            placeCall(str);
        }
    }


    public boolean can(String str) {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(str) == 0;
    }


    public void copyNumber(String str) {
        try {
            ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("phone", str));
            Toast.makeText(this, R.string.copied, 0).show();
        } catch (Exception unused) {
        }
    }

    private void dialVoicemail() {
        try {
            String voiceMailNumber = ((TelephonyManager) getSystemService("phone")).getVoiceMailNumber();
            if (voiceMailNumber == null || voiceMailNumber.isEmpty()) {
                Toast.makeText(this, R.string.voicemail_not_set, 0).show();
            } else {
                callNumber(voiceMailNumber);
            }
        } catch (Exception unused) {
            Toast.makeText(this, R.string.voicemail_not_set, 0).show();
        }
    }

    private static String digitsOnly(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int dtmfTone(String str) {
        if ("0".equals(str)) {
            return 0;
        }
        if ("1".equals(str)) {
            return 1;
        }
        if ("2".equals(str)) {
            return 2;
        }
        if ("3".equals(str)) {
            return 3;
        }
        if ("4".equals(str)) {
            return 4;
        }
        if ("5".equals(str)) {
            return 5;
        }
        if ("6".equals(str)) {
            return 6;
        }
        if ("7".equals(str)) {
            return 7;
        }
        if ("8".equals(str)) {
            return 8;
        }
        if ("9".equals(str)) {
            return 9;
        }
        if ("*".equals(str)) {
            return 10;
        }
        return "#".equals(str) ? 11 : 1;
    }


    public void editContact(long j) {
        try {
            startActivity(new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, j)));
        } catch (Exception unused) {
        }
    }


    public ContactEntry findContactByNumber(String str) {
        String digitsOnly = digitsOnly(str);
        if (digitsOnly.isEmpty()) {
            return null;
        }
        for (ContactEntry contactEntry : this.allContacts) {
            String digitsOnly2 = digitsOnly(contactEntry.number);
            if (digitsOnly2.length() >= digitsOnly.length() && digitsOnly2.endsWith(digitsOnly)) {
                return contactEntry;
            }
        }
        return null;
    }


    public String formatDuration(long j) {
        String valueOf;
        long j2 = j / 60;
        long j3 = j % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(j2);
        sb.append(":");
        if (j3 < 10) {
            valueOf = "0" + j3;
        } else {
            valueOf = String.valueOf(j3);
        }
        sb.append(valueOf);
        return sb.toString();
    }

    private Set<String> getAllowedAccounts() {
        Set<String> stringSet = getPrefs().getStringSet("accounts", null);
        if (stringSet == null || stringSet.isEmpty()) {
            return null;
        }
        /* v141: normalize legacy "type|name" keys to type-only */
        HashSet<String> normalized = new HashSet<>();
        for (String s : stringSet) {
            if (s == null) {
                continue;
            }
            int bar = s.indexOf('|');
            normalized.add(bar >= 0 ? s.substring(0, bar) : s);
        }
        return normalized;
    }

    private Set<String> getBlockedNumbers() {
        Set<String> stringSet = getPrefs().getStringSet("blocked", null);
        return stringSet == null ? new HashSet() : new HashSet(stringSet);
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


    public SharedPreferences getPrefs() {
        return getSharedPreferences("mydialer", 0);
    }

    private void handleDialIntent() {
        Intent intent = getIntent();
        if (intent == null || intent.getData() == null || !"tel".equals(intent.getData().getScheme())) {
            return;
        }
        this.numberText.setText(intent.getData().getSchemeSpecificPart());
        selectTab(0);
    }


    public boolean handleKeyLongPress(String str) {
        if ("1".equals(str)) {
            dialVoicemail();
            return true;
        }
        try {
            int parseInt = Integer.parseInt(str);
            if (parseInt >= 2 && parseInt <= 9) {
                String string = getPrefs().getString("speed" + parseInt, null);
                if (string == null || !string.contains("|")) {
                    this.pendingSpeedSlot = parseInt;
                    try {
                        startActivityForResult(new Intent("android.intent.action.PICK", ContactsContract.CommonDataKinds.Phone.CONTENT_URI), REQ_PICK_SPEED);
                    } catch (Exception unused) {
                        Toast.makeText(this, "Could not open contacts", 0).show();
                    }
                } else {
                    callNumber(string.substring(string.indexOf(124) + 1));
                }
                return true;
            }
        } catch (NumberFormatException unused2) {
        }
        return false;
    }

    private void handleVoiceInput(String str) {
        if (str == null || str.trim().isEmpty()) {
            return;
        }
        String trim = str.trim();
        StringBuilder sb = new StringBuilder();
        for (char c : trim.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.length() >= 3) {
            this.numberText.setText(sb.toString());
            callNumber(sb.toString());
            return;
        }
        String lowerCase = trim.toLowerCase(Locale.US);
        String str2 = lowerCase.split("\\s+")[0];
        for (ContactEntry contactEntry : this.allContacts) {
            if (contactEntry.name != null && contactEntry.name.toLowerCase(Locale.US).contains(lowerCase)) {
                Toast.makeText(this, getString(R.string.voice_calling) + " " + contactEntry.name, 0).show();
                callNumber(contactEntry.number);
                return;
            }
        }
        for (ContactEntry contactEntry2 : this.allContacts) {
            if (contactEntry2.name != null && contactEntry2.name.toLowerCase(Locale.US).contains(str2)) {
                Toast.makeText(this, getString(R.string.voice_calling) + " " + contactEntry2.name, 0).show();
                callNumber(contactEntry2.number);
                return;
            }
        }
        Toast.makeText(this, R.string.voice_no_match, 0).show();
    }

    private String international(String str) {
        String digitsOnly = digitsOnly(str);
        if (digitsOnly.length() != 10) {
            return digitsOnly;
        }
        return "91" + digitsOnly;
    }

    private boolean isBlocked(String str) {
        return getBlockedNumbers().contains(digitsOnly(str));
    }


    public void keyFeedback(String str) {
        if (getPrefs().getBoolean("key_vibrate", true)) {
            try {
                Vibrator vibrator = (Vibrator) getSystemService("vibrator");
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(20L);
                }
            } catch (Exception unused) {
            }
        }
        if (getPrefs().getBoolean("key_beeps", true)) {
            try {
                if (this.toneGenerator == null) {
                    this.toneGenerator = new ToneGenerator(8, 70);
                }
                this.toneGenerator.startTone(dtmfTone(str), 90);
            } catch (Exception unused2) {
            }
        }
    }


    public static String lastWord(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        int lastIndexOf = trim.lastIndexOf(32);
        return lastIndexOf >= 0 ? trim.substring(lastIndexOf + 1) : trim;
    }

    private void loadAccountMap() {
        this.accountByContact.clear();
        if (can("android.permission.READ_CONTACTS")) {
            try {
                Cursor query = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"contact_id", "account_type", "account_name"}, "contact_id NOT NULL", null, null);
                if (query != null) {
                    while (query.moveToNext()) {
                        long j = query.getLong(0);
                        if (!this.accountByContact.containsKey(Long.valueOf(j))) {
                            this.accountByContact.put(Long.valueOf(j), accountKey(query.getString(1), query.getString(2)));
                        }
                    }
                    query.close();
                }
            } catch (Exception unused) {
            }
        }
    }

    private void loadCalls() {
        String string;
        Cursor query;
        this.callEntries.clear();
        int i = 0;
        if (can("android.permission.READ_CALL_LOG")) {
            try {
                query = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"name", "number", "date", "type", "duration", "phone_account_id"}, null, null, "date DESC");
            } catch (Exception eCol) {
                query = null;
                try {
                    query = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{"name", "number", "date", "type", "duration"}, null, null, "date DESC");
                } catch (Exception eCol2) {
                    query = null;
                }
            }
            if (query != null) {
                int i2 = 0;
                while (query.moveToNext()) {
                    String string2 = query.getString(1);
                    if (string2 != null) {
                        int i3 = query.getInt(3);
                        if (i3 == 3) {
                            i2++;
                        }
                        String pa = null;
                        try {
                            if (query.getColumnCount() > 5) {
                                pa = query.getString(5);
                            }
                        } catch (Exception ePa) {
                            pa = null;
                        }
                        this.callEntries.add(new CallEntry(query.getString(0), string2, query.getLong(2), i3, query.getLong(4), pa));
                    }
                }
                query.close();
                i = i2;
            }
        }
        TextView textView = this.tabRecents;
        if (textView != null) {
            if (i > 0) {
                string = getString(R.string.tab_recents) + " (" + (i > 99 ? "99+" : String.valueOf(i)) + ")";
            } else {
                string = getString(R.string.tab_recents);
            }
            textView.setText(string);
        }
        try {
            TextView missedText = (TextView) this.recentsView.findViewById(R.id.missedChip);
            if (missedText != null && i > 0) {
                missedText.setText(getString(R.string.filter_missed) + " (" + (i > 99 ? "99+" : String.valueOf(i)) + ")");
            }
        } catch (Exception eMissed) {
        }
    }

    private void loadContacts() {
        this.allContacts.clear();
        loadAccountMap();
        this.allowedAccounts = getAllowedAccounts();
        queryContacts(null, this.allContacts);
        String loadProfileName = loadProfileName();
        if (loadProfileName != null) {
            this.allContacts.add(0, new ContactEntry(loadProfileName, getString(R.string.view_profile), -2L));
        }
    }

    private void loadFavourites() {
        this.favouriteContacts.clear();
        queryContacts("starred=1", this.favouriteContacts);
    }

    private String loadProfileName() {
        try {
            Cursor query = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
            if (query == null) {
                return null;
            }
            String string = query.moveToFirst() ? query.getString(0) : null;
            query.close();
            if (string != null && !string.trim().isEmpty()) {
                return string;
            }
            return getString(R.string.your_info);
        } catch (Exception unused) {
            return null;
        }
    }


    public void loadThreads() {
        View chipsRow = (View) this.messagesView.findViewById(R.id.chipMsgAll).getParent();
        if (chipsRow != null) {
            chipsRow.setVisibility(getPrefs().getBoolean("msg_chips", true) ? 0 : 8);
        }
        if (Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_SMS") == 0) {
            new Thread(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    MessageSettingsActivity.cleanExpiredOtps(MainActivity.this);
                    final ArrayList arrayList = new ArrayList();
                    try {
                        Cursor query = MainActivity.this.getContentResolver().query(Uri.parse("content://sms"), new String[]{"thread_id", "address", "body", "date", "read"}, null, null, "date DESC");
                        if (query != null) {
                            LinkedHashMap linkedHashMap = new LinkedHashMap();
                            while (query.moveToNext()) {
                                long j = query.getLong(0);
                                String string = query.getString(1);
                                String string2 = query.getString(2);
                                boolean z = query.getInt(4) == 0;
                                Object[] objArr = (Object[]) linkedHashMap.get(Long.valueOf(j));
                                if (objArr == null) {
                                    Long valueOf = Long.valueOf(j);
                                    Object[] objArr2 = new Object[7];
                                    objArr2[0] = Long.valueOf(j);
                                    objArr2[1] = string;
                                    objArr2[2] = string2;
                                    objArr2[3] = Long.valueOf(query.getLong(3));
                                    objArr2[4] = Boolean.valueOf(z);
                                    objArr2[5] = Integer.valueOf(z ? 1 : 0);
                                    objArr2[6] = Integer.valueOf(MainActivity.threadCategory(string, string2));
                                    linkedHashMap.put(valueOf, objArr2);
                                } else if (z) {
                                    objArr[5] = Integer.valueOf(((Integer) objArr[5]).intValue() + 1);
                                }
                            }
                            query.close();
                            arrayList.addAll(linkedHashMap.values());
                        }
                    } catch (Exception unused) {
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            ListView listView = (ListView) MainActivity.this.messagesView.findViewById(R.id.threadsList);
                            TextView textView = (TextView) MainActivity.this.messagesView.findViewById(R.id.messagesEmpty);
                            ArrayList arrayList2 = new ArrayList();
                            Iterator it = arrayList.iterator();
                            boolean spamOn = MainActivity.this.getPrefs().getBoolean("msg_spam_block", false);
                            while (it.hasNext()) {
                                Object[] objArr3 = (Object[]) it.next();
                                if (spamOn && ((Integer) objArr3[6]).intValue() == 2) {
                                    continue;
                                }
                                if (MainActivity.this.messagesFilter == 0 || MainActivity.this.messagesFilter == ((Integer) objArr3[6]).intValue() + 1) {
                                    arrayList2.add(objArr3);
                                }
                            }
                            if (arrayList2.isEmpty()) {
                                listView.setAdapter((ListAdapter) null);
                                textView.setVisibility(0);
                            } else {
                                textView.setVisibility(8);
                                listView.setAdapter((ListAdapter) new ThreadsAdapter(arrayList2));
                            }
                        }
                    });
                }
            }).start();
            return;
        }
        requestPermissions(new String[]{"android.permission.READ_SMS"}, 105);
        ((ListView) this.messagesView.findViewById(R.id.threadsList)).setAdapter((ListAdapter) null);
        this.messagesView.findViewById(R.id.messagesEmpty).setVisibility(0);
    }


    public void markSpam(String str) {
        Tags.set(this, str, getString(R.string.spam_tag));
        Set<String> blockedNumbers = getBlockedNumbers();
        blockedNumbers.add(digitsOnly(str));
        setBlockedNumbers(blockedNumbers);
        Toast.makeText(this, R.string.spam_marked, 0).show();
        applyCurrentFilter();
        updateDialpadMatch();
    }

    private boolean matches(String str, String str2, String str3) {
        if (str.isEmpty()) {
            return true;
        }
        if (str2 == null || !str2.toLowerCase(Locale.US).contains(str)) {
            return str3 != null && str3.toLowerCase(Locale.US).contains(str);
        }
        return true;
    }


    public void maybeLockApp() {
        if (unlockedThisSession) {
            return;
        }
        if (getPrefs().getString("app_pin", null) == null && getPrefs().getString("app_pattern", null) == null) {
            return;
        }
        try {
            startActivityForResult(new Intent(this, LockActivity.class), 77);
        } catch (Exception unusedLock) {
        }
    }

    private void maybePromptDefaultDialer() {
        try {
            String defaultDialerPackage = ((TelecomManager) getSystemService("telecom")).getDefaultDialerPackage();
            if (defaultDialerPackage != null) {
                if (defaultDialerPackage.equals(getPackageName())) {
                    return;
                }
            }
        } catch (Exception unused) {
        }
        if (Build.VERSION.SDK_INT < 29) {
            M3Util.infoDialog(this, R.string.set_default_manual_title, R.string.set_default_manual_msg);
            return;
        }
        try {
            final RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable("android.app.role.DIALER") && !roleManager.isRoleHeld("android.app.role.DIALER")) {
                new AlertDialog.Builder(this).setTitle(R.string.set_default_title).setMessage(R.string.set_default_msg).setPositiveButton(R.string.set_default_yes, new DialogInterface.OnClickListener() {
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            MainActivity.this.startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.DIALER"), 200);
                        } catch (Exception unused2) {
                        }
                    }
                }).setNegativeButton(R.string.set_default_later, (DialogInterface.OnClickListener) null).show();
            }
        } catch (Exception unused2) {
        }
    }


    public String numberTypeLabel(String str) {
        String digitsOnly = digitsOnly(str);
        if (digitsOnly.startsWith("140")) {
            return "  |  " + getString(R.string.telemarketing);
        }
        if (!digitsOnly.startsWith("1800") && !digitsOnly.startsWith("1860") && !digitsOnly.startsWith("1861")) {
            return "";
        }
        return "  |  " + getString(R.string.toll_free);
    }


    public void openArattai(String str) {
        Intent launchIntentForPackage;
        try {
            copyNumber(str);
            for (ApplicationInfo applicationInfo : getPackageManager().getInstalledApplications(0)) {
                if (applicationInfo.packageName.toLowerCase().contains("arattai") && (launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(applicationInfo.packageName)) != null) {
                    startActivity(launchIntentForPackage);
                    return;
                }
            }
            Toast.makeText(this, "Arattai " + getString(R.string.not_installed), 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, "Arattai " + getString(R.string.not_installed), 0).show();
        }
    }


    public void openEmailFor(String str) {
        Cursor query;
        try {
            ContactEntry findContactByNumber = findContactByNumber(str);
            if (findContactByNumber != null && findContactByNumber.contactId > 0 && (query = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, new String[]{"data1"}, "contact_id=?", new String[]{String.valueOf(findContactByNumber.contactId)}, null)) != null) {
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


    public void openOwnProfile() {
        try {
            startActivity(new Intent("android.intent.action.VIEW", ContactsContract.Profile.CONTENT_URI));
        } catch (Exception unused) {
            Toast.makeText(this, R.string.profile_not_set, 0).show();
        }
    }


    public void openTelegram(String str) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("tg://resolve?phone=" + international(str)));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage("org.telegram.messenger");
            if (launchIntentForPackage != null) {
                startActivity(launchIntentForPackage);
                return;
            }
            Toast.makeText(this, "Telegram " + getString(R.string.not_installed), 0).show();
        } catch (Exception unused) {
            Toast.makeText(this, "Telegram " + getString(R.string.not_installed), 0).show();
        }
    }


    public void openWhatsApp(String str) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + international(str))));
        } catch (Exception unused) {
            Toast.makeText(this, "WhatsApp " + getString(R.string.not_installed), 0).show();
        }
    }


    public void placeCall(String str) {
        if (getPrefs().getBoolean("private_calling", false) && str != null && !str.trim().isEmpty() && !str.startsWith("*31#")) {
            str = "*31#" + str;
            Toast.makeText(this, R.string.private_call_toast, 0).show();
        }
        Uri parse = Uri.parse("tel:" + Uri.encode(str));
        if (!can("android.permission.CALL_PHONE")) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{"android.permission.CALL_PHONE"}, 101);
            }
            startActivity(new Intent("android.intent.action.DIAL", parse));
            return;
        }
        TelecomManager telecomManager = (TelecomManager) getSystemService("telecom");
        if (can("android.permission.READ_PHONE_STATE")) {
            try {
                List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
                if (callCapablePhoneAccounts != null && callCapablePhoneAccounts.size() > 1) {
                    int i = getPrefs().getInt("preferred_sim", -1);
                    if (i == 99) {
                        showSimPicker(telecomManager, parse, callCapablePhoneAccounts, false);
                        return;
                    }
                    if (i >= 0 && i < callCapablePhoneAccounts.size()) {
                        placeCallWith(telecomManager, parse, callCapablePhoneAccounts.get(i));
                        return;
                    }
                    PhoneAccountHandle defaultOutgoingPhoneAccount = telecomManager.getDefaultOutgoingPhoneAccount("tel");
                    if (defaultOutgoingPhoneAccount == null || callCapablePhoneAccounts.indexOf(defaultOutgoingPhoneAccount) < 0) {
                        showSimPicker(telecomManager, parse, callCapablePhoneAccounts, false);
                        return;
                    } else {
                        placeCallWith(telecomManager, parse, defaultOutgoingPhoneAccount);
                        return;
                    }
                }
            } catch (Exception unused) {
            }
        }
        startActivity(new Intent("android.intent.action.CALL", parse));
    }


    public void placeCallWith(TelecomManager telecomManager, Uri uri, PhoneAccountHandle phoneAccountHandle) {
        try {
            try {
                Bundle bundle = new Bundle();
                bundle.putParcelable("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
                telecomManager.placeCall(uri, bundle);
            } catch (Exception unused) {
                startActivity(new Intent("android.intent.action.CALL", uri));
            }
        } catch (Exception unused2) {
        }
    }

    private void queryContacts(String str, List<ContactEntry> list) {
        String str2;
        if (can("android.permission.READ_CONTACTS")) {
            Cursor query = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "data1", "contact_id"}, str, null, "display_name ASC");
            if (query != null) {
                ArrayList arrayList = new ArrayList();
                while (query.moveToNext()) {
                    String string = query.getString(0);
                    String string2 = query.getString(1);
                    if (string != null && string2 != null) {
                        String digitsOnly = digitsOnly(string2);
                        if (!digitsOnly.isEmpty() && !arrayList.contains(digitsOnly)) {
                            long j = query.getLong(2);
                            Set<String> set = this.allowedAccounts;
                            if (set == null || set.isEmpty() || ((str2 = this.accountByContact.get(Long.valueOf(j))) != null && this.allowedAccounts.contains(str2))) {
                                arrayList.add(digitsOnly);
                                list.add(new ContactEntry(string, string2, j));
                            }
                        }
                    }
                }
                query.close();
            }
            if ("last".equals(getPrefs().getString("sort_by", "first"))) {
                Collections.sort(list, new Comparator<ContactEntry>() {
                    @Override // java.util.Comparator
                    public int compare(ContactEntry contactEntry, ContactEntry contactEntry2) {
                        return MainActivity.lastWord(contactEntry.name).compareToIgnoreCase(MainActivity.lastWord(contactEntry2.name));
                    }
                });
            }
        }
    }

    private void saveSpeedDial(Uri uri, int i) {
        try {
            Cursor query = getContentResolver().query(uri, new String[]{"display_name", "data1"}, null, null, null);
            if (query != null) {
                if (query.moveToFirst()) {
                    String string = query.getString(0);
                    String string2 = query.getString(1);
                    if (string == null) {
                        string = getString(R.string.unknown_caller);
                    }
                    if (string2 != null && !string2.isEmpty()) {
                        getPrefs().edit().putString("speed" + i, string + "|" + string2).commit();
                        Toast.makeText(this, R.string.speed_dial_set, 0).show();
                    }
                    Toast.makeText(this, "Contact has no number", 0).show();
                }
                query.close();
            }
        } catch (Exception unused) {
        }
    }


    private void loadHeaderAvatar() {
        try {
            android.widget.ImageButton ib = (android.widget.ImageButton) findViewById(R.id.profileHeader);
            if (ib == null) {
                return;
            }
            java.io.File file = new java.io.File(getFilesDir(), "profile.jpg");
            if (file.exists()) {
                android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                if (b != null) {
                    ib.setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), b));
                    ib.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override // android.view.ViewOutlineProvider
                        public void getOutline(android.view.View view, android.graphics.Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    });
                    ib.setClipToOutline(true);
                    return;
                }
            }
            ib.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        } catch (Throwable unused) {
        }
    }

    public void selectTab(int i) {
        if (i < 0 || i > 4) {
            i = 0;
        }
        if (i == 3 && !getPrefs().getBoolean("show_favourites", true)) {
            i = 0;
        }
        if (i == 0) {
            i = 1;
        }
        this.currentTab = i;
        getPrefs().edit().putInt("last_tab", i).commit();
        this.content.removeAllViews();
        if (i == 1) {
            try {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(1);
                linearLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                if (this.recentsView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) this.recentsView.getParent()).removeView(this.recentsView);
                }
                if (this.dialpadView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) this.dialpadView.getParent()).removeView(this.dialpadView);
                }
                this.recentsView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
                this.dialpadView.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                linearLayout.addView(this.recentsView);
                linearLayout.addView(this.dialpadView);
                this.content.addView(linearLayout);
            } catch (Throwable th) {
                this.content.removeAllViews();
                if (this.recentsView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) this.recentsView.getParent()).removeView(this.recentsView);
                }
                this.recentsView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                this.content.addView(this.recentsView);
            }
        } else {
            this.content.addView(i == 2 ? this.contactsView : i == 3 ? this.favouritesView : this.messagesView);
        }
        if (i == 0) {
            applyDialpadBackground();
            if (can("android.permission.READ_CONTACTS")) {
                loadContacts();
            } else if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{"android.permission.READ_CONTACTS"}, 102);
            }
            if (can("android.permission.READ_CALL_LOG")) {
                loadCalls();
            }
            updateDialpadMatch();
            updateSimBadge();
        }
        if (i == 1) {
            loadCalls();
        }
        if (i == 2) {
            loadContacts();
        }
        if (i == 3) {
            loadFavourites();
        }
        if (i == 4) {
            loadThreads();
        }
        applyCurrentFilter();
        TextView textView = this.headerTitle;
        if (textView != null) {
            if (i == 0) {
                textView.setText(R.string.phone_title);
            } else if (i == 1) {
                textView.setText(R.string.tab_recents);
            } else if (i == 2) {
                textView.setText(R.string.tab_contacts);
            } else if (i == 3) {
                textView.setText(R.string.tab_favourites);
            } else {
                textView.setText(R.string.tab_messages);
            }
        }
        this.addContactButton.setVisibility(i == 2 ? 0 : 8);
        android.widget.ImageButton msgSetBtn = (android.widget.ImageButton) findViewById(R.id.msgSettings);
        if (msgSetBtn != null) {
            msgSetBtn.setVisibility(i == 4 ? 0 : 8);
        }
        tintTab(this.tabRecents, i == 1);
        tintTab(this.tabContacts, i == 2);
        tintTab(this.tabFavourites, i == 3);
        tintTab(this.tabMessages, i == 4);
    }


    public void sendSms(String str) {
        try {
            startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + str)));
        } catch (Exception unused) {
        }
    }


    public void setAvatar(TextView textView, String str, long j) {
        Bitmap contactPhoto;
        if (j > 0 && getPrefs().getBoolean("contact_photos", false) && (contactPhoto = getContactPhoto(j)) != null) {
            textView.setText("");
            try {
                textView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override // android.view.ViewOutlineProvider
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                textView.setClipToOutline(true);
            } catch (Exception unused) {
            }
            textView.setBackground(new BitmapDrawable(getResources(), contactPhoto));
            return;
        }
        String trim = str != null ? str.trim() : "";
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(1);
        int[] iArr = AVATAR_COLORS;
        gradientDrawable.setColor(iArr[Math.abs(trim.toLowerCase(Locale.US).hashCode()) % iArr.length]);
        textView.setBackground(gradientDrawable);
        textView.setClipToOutline(false);
        textView.setText(trim.isEmpty() ? "?" : trim.substring(0, 1).toUpperCase(Locale.US));
    }

    /* Deletes a single call log entry and refreshes the Recents list */
    public void deleteCallEntry(final CallEntry callEntry) {
        try {
            if (!can("android.permission.WRITE_CALL_LOG")) {
                requestPermissions(new String[]{"android.permission.WRITE_CALL_LOG"}, 107);
                Toast.makeText(this, R.string.delete_perm_needed, 0).show();
                return;
            }
            int deleted = getContentResolver().delete(android.provider.CallLog.Calls.CONTENT_URI, "NUMBER=? AND DATE=? AND TYPE=?", new String[]{callEntry.number, String.valueOf(callEntry.date), String.valueOf(callEntry.type)});
            loadCalls();
            applyCurrentFilter();
            Toast.makeText(this, deleted > 0 ? R.string.call_log_removed : R.string.call_not_found, 0).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.call_not_found, 0).show();
        }
    }

    private void setBlockedNumbers(Set<String> set) {
        getPrefs().edit().putStringSet("blocked", set).commit();
    }

    private void showAfterCall() {
        final String str = CallState.lastCallNumber;
        if (str == null || str.isEmpty()) {
            return;
        }
        try {
            CallLogSync.insertIfMissing(this, str, CallState.lastCallMissed ? 3 : 2, 0L);
        } catch (Exception unused) {
        }
        String name = null;
        long contactId = -1;
        try {
            String dg = str.replaceAll("[^0-9]", "");
            if (dg.length() >= 5) {
                android.database.Cursor q = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"contact_id", "display_name", "data1"}, null, null, null);
                if (q != null) {
                    while (q.moveToNext()) {
                        String num = q.getString(2);
                        if (num != null && num.replaceAll("[^0-9]", "").equals(dg)) {
                            contactId = q.getLong(0);
                            name = q.getString(1);
                            break;
                        }
                    }
                    q.close();
                }
            }
        } catch (Exception unused) {
        }
        if (name == null) {
            String t = Tags.get(this, str);
            if (t != null) {
                name = t;
            }
        }
        Bitmap photo = null;
        if (contactId > 0) {
            try {
                photo = getContactPhoto(contactId);
            } catch (Exception unused) {
            }
        }
        String dur = "";
        try {
            android.database.Cursor q2 = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, new String[]{"number", "duration"}, null, null, "date DESC LIMIT 8");
            if (q2 != null) {
                String dg2 = str.replaceAll("[^0-9]", "");
                while (q2.moveToNext()) {
                    String n = q2.getString(0);
                    long d = q2.getLong(1);
                    if (n != null && d > 0 && n.replaceAll("[^0-9]", "").equals(dg2)) {
                        dur = formatDuration((int) d);
                        break;
                    }
                }
                q2.close();
            }
        } catch (Exception unused) {
        }
        String em = EmojiUtil.display(this, str);
        try {
            android.widget.LinearLayout card = M3Util.card(this);
            card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            if (photo != null) {
                android.widget.ImageView avImg = new android.widget.ImageView(this);
                avImg.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                avImg.setImageBitmap(photo);
                avImg.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override // android.view.ViewOutlineProvider
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                avImg.setClipToOutline(true);
                card.addView(avImg, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 64), M3Util.dp(this, 64)));
            } else if (name != null) {
                android.widget.TextView avatar = new android.widget.TextView(this);
                avatar.setText(name.substring(0, 1).toUpperCase(Locale.US));
                avatar.setTextColor(-1);
                avatar.setTextSize(24.0f);
                avatar.setGravity(android.view.Gravity.CENTER);
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                g.setColor(AVATAR_COLORS[Math.abs(name.toLowerCase(Locale.US).hashCode()) % AVATAR_COLORS.length]);
                avatar.setBackgroundDrawable(g);
                card.addView(avatar, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 64), M3Util.dp(this, 64)));
            } else {
                android.widget.LinearLayout avBox = new android.widget.LinearLayout(this);
                avBox.setGravity(android.view.Gravity.CENTER);
                android.graphics.drawable.GradientDrawable g2 = new android.graphics.drawable.GradientDrawable();
                g2.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                g2.setColor(0xff424242);
                avBox.setBackgroundDrawable(g2);
                android.widget.ImageView ic = new android.widget.ImageView(this);
                ic.setImageResource(R.drawable.ic_person_white);
                avBox.addView(ic, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 30), M3Util.dp(this, 30)));
                card.addView(avBox, new android.widget.LinearLayout.LayoutParams(M3Util.dp(this, 64), M3Util.dp(this, 64)));
            }
            android.widget.TextView titleTv = new android.widget.TextView(this);
            titleTv.setText(CallState.lastCallMissed ? R.string.after_missed : R.string.after_ended);
            titleTv.setTextSize(13.0f);
            titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            titleTv.setTextColor(getResources().getColor(R.color.text_hint));
            titleTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            tlp.topMargin = M3Util.dp(this, 14);
            card.addView(titleTv, tlp);
            if (name != null) {
                android.widget.TextView nameTv = new android.widget.TextView(this);
                nameTv.setText(em.isEmpty() ? name : em + " " + name);
                nameTv.setTextSize(20.0f);
                nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                nameTv.setTextColor(getResources().getColor(R.color.text_main));
                nameTv.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
                nlp.topMargin = M3Util.dp(this, 2);
                card.addView(nameTv, nlp);
            } else {
                android.widget.TextView numBig = new android.widget.TextView(this);
                numBig.setText(PrivacyUtil.mask(this, str));
                numBig.setTextSize(20.0f);
                numBig.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                numBig.setTextColor(getResources().getColor(R.color.text_main));
                numBig.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout.LayoutParams nlp2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
                nlp2.topMargin = M3Util.dp(this, 2);
                card.addView(numBig, nlp2);
            }
            String sub = PrivacyUtil.mask(this, str);
            if (name != null) {
                sub = sub + (dur.isEmpty() ? "" : "  |  " + dur);
            } else if (!dur.isEmpty()) {
                sub = dur;
            }
            android.widget.TextView numTv = new android.widget.TextView(this);
            numTv.setText(sub);
            numTv.setTextSize(14.0f);
            numTv.setTextColor(getResources().getColor(R.color.text_hint));
            numTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams ulp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            ulp.topMargin = M3Util.dp(this, 2);
            card.addView(numTv, ulp);
            if (name == null && dur.isEmpty()) {
                numTv.setVisibility(8);
            }
            android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
            btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = M3Util.dp(this, 22);
            android.widget.TextView smsBtn = M3Util.outlineBtn(this, getString(R.string.send_sms));
            btnRow.addView(smsBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView callBtn = M3Util.greenBtn(this, getString(R.string.call_action), R.drawable.ic_call_white);
            android.widget.LinearLayout.LayoutParams clp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            clp.leftMargin = M3Util.dp(this, 10);
            btnRow.addView(callBtn, clp);
            card.addView(btnRow, blp);
            android.widget.LinearLayout btnRow2 = new android.widget.LinearLayout(this);
            btnRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnRow2.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams b2lp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            b2lp.topMargin = M3Util.dp(this, 10);
            android.widget.TextView videoBtn = M3Util.outlineBtnIcon(this, getString(R.string.video_call), R.drawable.ic_videocam);
            btnRow2.addView(videoBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView waBtn = M3Util.outlineBtnIcon(this, getString(R.string.whatsapp), R.drawable.ic_whatsapp);
            android.widget.LinearLayout.LayoutParams wlp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            wlp.leftMargin = M3Util.dp(this, 10);
            btnRow2.addView(waBtn, wlp);
            card.addView(btnRow2, b2lp);
            final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
            if (SpamUtil.isSpam(this, str)) {
                android.widget.TextView spamBadgeTv = new android.widget.TextView(this);
                spamBadgeTv.setText(SpamUtil.getCategory(this, str));
                spamBadgeTv.setTextSize(13.0f);
                spamBadgeTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                spamBadgeTv.setTextColor(getResources().getColor(R.color.missed_red));
                spamBadgeTv.setGravity(android.view.Gravity.CENTER);
                android.widget.LinearLayout.LayoutParams slp2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
                slp2.topMargin = M3Util.dp(this, 14);
                card.addView(spamBadgeTv, slp2);
            } else {
                String susAc = SpamUtil.getSuspected(this, str);
                if (susAc != null) {
                    android.widget.TextView susBadgeTv = new android.widget.TextView(this);
                    susBadgeTv.setText(susAc);
                    susBadgeTv.setTextSize(13.0f);
                    susBadgeTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    susBadgeTv.setTextColor(0xffff9800);
                    susBadgeTv.setGravity(android.view.Gravity.CENTER);
                    android.widget.LinearLayout.LayoutParams slp0 = new android.widget.LinearLayout.LayoutParams(-1, -2);
                    slp0.topMargin = M3Util.dp(this, 14);
                    card.addView(susBadgeTv, slp0);
                }
                android.widget.TextView spamBtn = new android.widget.TextView(this);
                spamBtn.setText(R.string.mark_spam_full);
                spamBtn.setTextSize(14.0f);
                spamBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                spamBtn.setTextColor(0xffff5252);
                spamBtn.setGravity(android.view.Gravity.CENTER);
                spamBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_input_outline));
                spamBtn.setPadding(M3Util.dp(this, 16), M3Util.dp(this, 10), M3Util.dp(this, 16), M3Util.dp(this, 10));
                spamBtn.setClickable(true);
                android.widget.LinearLayout.LayoutParams slp3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
                slp3.topMargin = M3Util.dp(this, 12);
                card.addView(spamBtn, slp3);
                spamBtn.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        SpamUtil.add(MainActivity.this, str);
                        try { dlg.dismiss(); } catch (Exception e) { }
                        Toast.makeText(MainActivity.this, R.string.spam_saved, 0).show();
                    }
                });
            }
            smsBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                    MainActivity.this.sendSms(str);
                }
            });
            callBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                    MainActivity.this.callNumber(str);
                }
            });
            videoBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                    MainActivity.this.startVideoCall(str);
                }
            });
            waBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                    MainActivity.this.openWhatsApp(str);
                }
            });
            M3Util.scrim(dlg);
        } catch (Exception e) {
            callNumber(str);
        }
    }

    /* Attempt a system (VoLTE) video call */
    private void startVideoCall(String str) {
        try {
            if (!can("android.permission.CALL_PHONE")) {
                Toast.makeText(this, R.string.video_not_supported, 0).show();
                return;
            }
            Intent intent = new Intent("android.intent.action.CALL", Uri.parse("tel:" + Uri.encode(str)));
            intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3);
            startActivity(intent);
        } catch (Exception unused) {
            Toast.makeText(this, R.string.video_not_supported, 0).show();
        }
    }


    public void showNumberOptions(String str, final String str2) {
        final boolean isBlocked = isBlocked(str2);
        final ContactEntry findContactByNumber = findContactByNumber(str2);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (str == null || str.trim().isEmpty()) {
            str = str2;
        }
        builder.setTitle(str);
        CharSequence[] charSequenceArr = new CharSequence[11];
        charSequenceArr[0] = getString(R.string.call_action);
        charSequenceArr[1] = getString(R.string.send_sms);
        charSequenceArr[2] = getString(R.string.copy_number);
        charSequenceArr[3] = getString(findContactByNumber != null ? R.string.edit_contact : R.string.add_contact);
        charSequenceArr[4] = getString(isBlocked ? R.string.unblock_number : R.string.block_number);
        charSequenceArr[5] = getString(R.string.whatsapp);
        charSequenceArr[6] = getString(R.string.telegram);
        charSequenceArr[7] = getString(R.string.arattai);
        charSequenceArr[8] = getString(R.string.send_email);
        charSequenceArr[9] = getString(R.string.add_tag);
        charSequenceArr[10] = getString(R.string.mark_spam);
        builder.setItems(charSequenceArr, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    MainActivity.this.callNumber(str2);
                    return;
                }
                if (i == 1) {
                    MainActivity.this.sendSms(str2);
                    return;
                }
                if (i == 2) {
                    MainActivity.this.copyNumber(str2);
                    return;
                }
                if (i == 5) {
                    MainActivity.this.openWhatsApp(str2);
                    return;
                }
                if (i == 6) {
                    MainActivity.this.openTelegram(str2);
                    return;
                }
                if (i == 7) {
                    MainActivity.this.openArattai(str2);
                    return;
                }
                if (i == 8) {
                    MainActivity.this.openEmailFor(str2);
                    return;
                }
                if (i == 9) {
                    MainActivity.this.showTagDialog(str2);
                    return;
                }
                if (i == 10) {
                    MainActivity.this.markSpam(str2);
                    return;
                }
                if (i != 3) {
                    if (isBlocked) {
                        MainActivity.this.unblockNumber(str2);
                        return;
                    } else {
                        MainActivity.this.blockNumber(str2);
                        return;
                    }
                }
                ContactEntry contactEntry = findContactByNumber;
                if (contactEntry != null) {
                    MainActivity.this.editContact(contactEntry.contactId);
                } else {
                    MainActivity.this.addToContacts(str2);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
        builder.show();
    }


    public void showSimPicker(final TelecomManager telecomManager, final Uri uri, final List<PhoneAccountHandle> list, final boolean z) {
        final CharSequence[] charSequenceArr = new CharSequence[list.size()];
        int i = 0;
        while (i < list.size()) {
            StringBuilder sb = new StringBuilder("SIM ");
            int i2 = i + 1;
            sb.append(i2);
            String sb2 = sb.toString();
            try {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(list.get(i));
                if (phoneAccount != null && phoneAccount.getLabel() != null) {
                    sb2 = phoneAccount.getLabel().toString();
                }
            } catch (Exception unused) {
            }
            charSequenceArr[i] = sb2;
            i = i2;
        }
        new AlertDialog.Builder(this).setTitle(R.string.pick_sim).setItems(charSequenceArr, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i3) {
                MainActivity.this.getPrefs().edit().putInt("preferred_sim", i3).commit();
                MainActivity.this.updateSimBadge();
                if (!z) {
                    MainActivity.this.placeCallWith(telecomManager, uri, (PhoneAccountHandle) list.get(i3));
                    return;
                }
                Toast.makeText(MainActivity.this, ((Object) charSequenceArr[i3]) + " selected", 0).show();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }


    public void showTagDialog(final String str) {
        final EditText editText = new EditText(this);
        String str2 = Tags.get(this, str);
        if (str2 != null) {
            editText.setText(str2);
        }
        new AlertDialog.Builder(this).setTitle(R.string.add_tag).setMessage(R.string.add_tag_msg).setView(editText).setPositiveButton(R.string.save_tag, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                String trim = editText.getText().toString().trim();
                Tags.set(MainActivity.this, str, trim);
                Toast.makeText(MainActivity.this, trim.isEmpty() ? R.string.tag_removed : R.string.tag_saved, 0).show();
                MainActivity.this.applyCurrentFilter();
                MainActivity.this.updateDialpadMatch();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private boolean t9NameMatch(String str, String str2) {
        boolean z;
        if (str != null && !str2.isEmpty()) {
            String replaceAll = str.toLowerCase(Locale.US).replaceAll("[^a-z]", "");
            int length = str2.length();
            if (replaceAll.length() < length) {
                return false;
            }
            for (int i = 0; i + length <= replaceAll.length(); i++) {
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        z = true;
                        break;
                    }
                    if (T9[str2.charAt(i2) - '0'].indexOf(replaceAll.charAt(i + i2)) < 0) {
                        z = false;
                        break;
                    }
                    i2++;
                }
                if (z) {
                    return true;
                }
            }
        }
        return false;
    }


    public static int threadCategory(String str, String str2) {
        String trim = str == null ? "" : str.trim();
        String lowerCase = str2 == null ? "" : str2.toLowerCase(Locale.US);
        String replaceAll = trim.replaceAll("\\D", "");
        boolean z = replaceAll.length() >= 7 && replaceAll.length() == trim.replace("+", "").trim().length();
        if (!(lowerCase.contains("otp") || lowerCase.contains("one time") || lowerCase.contains("verification") || lowerCase.contains("verify") || lowerCase.contains("your code") || lowerCase.contains("password")) || z) {
            return z ? 0 : 2;
        }
        return 1;
    }


    public void tintFilterChips() {
        TextView textView = (TextView) this.recentsView.findViewById(R.id.allChip);
        TextView textView2 = (TextView) this.recentsView.findViewById(R.id.missedChip);
        TextView textView3 = (TextView) this.recentsView.findViewById(R.id.incomingChip);
        TextView textView4 = (TextView) this.recentsView.findViewById(R.id.outgoingChip);
        int i = this.filterMode;
        int i2 = R.drawable.bg_chip_active;
        textView.setBackgroundResource(i == 0 ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        textView.setTextColor(this.filterMode == 0 ? -1 : -6381922);
        textView2.setBackgroundResource(this.filterMode == 1 ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        textView2.setTextColor(this.filterMode == 1 ? -1 : -6381922);
        textView3.setBackgroundResource(this.filterMode == 2 ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        textView3.setTextColor(this.filterMode == 2 ? -1 : -6381922);
        if (this.filterMode != 3) {
            i2 = R.drawable.bg_chip_inactive;
        }
        textView4.setBackgroundResource(i2);
        textView4.setTextColor(this.filterMode != 3 ? -6381922 : -1);
    }


    public void tintMessageChips() {
        int[][] iArr = {new int[]{R.id.chipMsgAll, 0}, new int[]{R.id.chipMsgPersonal, 1}, new int[]{R.id.chipMsgOtp, 2}, new int[]{R.id.chipMsgOthers, 3}};
        for (int i = 0; i < 4; i++) {
            int[] iArr2 = iArr[i];
            TextView textView = (TextView) this.messagesView.findViewById(iArr2[0]);
            textView.setBackgroundResource(this.messagesFilter == iArr2[1] ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            textView.setTextColor(this.messagesFilter == iArr2[1] ? -1 : getResources().getColor(R.color.text_hint));
        }
    }

    private void tintTab(TextView textView, boolean z) {
        Drawable drawable;
        int i = z ? -1 : -6381922;
        textView.setTextColor(i);
        textView.setBackgroundResource(z ? R.drawable.bg_tab_circle : 0);
        if (z && textView.getBackground() != null) {
            try {
                textView.getBackground().mutate().setTint(getPrefs().getInt("accent", -3129167));
            } catch (Exception unused) {
            }
        }
        Drawable[] compoundDrawables = textView.getCompoundDrawables();
        if (compoundDrawables == null || (drawable = compoundDrawables[1]) == null) {
            return;
        }
        Drawable mutate = drawable.mutate();
        mutate.setColorFilter(i, PorterDuff.Mode.SRC_IN);
        textView.setCompoundDrawables(compoundDrawables[0], mutate, compoundDrawables[2], compoundDrawables[3]);
    }


    public String typeLabel(int i) {
        return i == 1 ? getString(R.string.type_incoming) : i == 2 ? getString(R.string.type_outgoing) : i == 3 ? getString(R.string.type_missed) : i == 5 ? getString(R.string.type_rejected) : getString(R.string.type_other);
    }


    public void unblockNumber(String str) {
        Set<String> blockedNumbers = getBlockedNumbers();
        blockedNumbers.remove(digitsOnly(str));
        setBlockedNumbers(blockedNumbers);
        Toast.makeText(this, R.string.unblocked_toast, 0).show();
    }


    public void updateDialpadMatch() {
        String digitsOnly = digitsOnly(this.numberText.getText().toString());
        this.matchedName = null;
        this.matchedNumber = null;
        this.dialpadSuggestions.clear();
        if (digitsOnly.length() >= 2) {
            HashSet hashSet = new HashSet();
            for (ContactEntry contactEntry : this.allContacts) {
                String digitsOnly2 = digitsOnly(contactEntry.number);
                if (digitsOnly2.length() >= digitsOnly.length() && digitsOnly2.endsWith(digitsOnly)) {
                    this.dialpadSuggestions.add(contactEntry);
                    hashSet.add(contactEntry);
                    if (this.dialpadSuggestions.size() >= 24) {
                        break;
                    }
                }
            }
            if (this.dialpadSuggestions.size() < 24) {
                for (ContactEntry contactEntry2 : this.allContacts) {
                    if (!hashSet.contains(contactEntry2) && digitsOnly(contactEntry2.number).contains(digitsOnly)) {
                        this.dialpadSuggestions.add(contactEntry2);
                        hashSet.add(contactEntry2);
                        if (this.dialpadSuggestions.size() >= 24) {
                            break;
                        }
                    }
                }
            }
            if (this.dialpadSuggestions.size() < 24) {
                for (ContactEntry contactEntry3 : this.allContacts) {
                    if (!hashSet.contains(contactEntry3) && t9NameMatch(contactEntry3.name, digitsOnly)) {
                        this.dialpadSuggestions.add(contactEntry3);
                        if (this.dialpadSuggestions.size() >= 24) {
                            break;
                        }
                    }
                }
            }
            if (this.dialpadSuggestions.size() < 24) {
                HashSet hashSet2 = new HashSet();
                Iterator<ContactEntry> it = this.dialpadSuggestions.iterator();
                while (it.hasNext()) {
                    hashSet2.add(digitsOnly(it.next().number));
                }
                for (CallEntry callEntry : this.callEntries) {
                    String digitsOnly3 = digitsOnly(callEntry.number);
                    if (!digitsOnly3.isEmpty() && !hashSet2.contains(digitsOnly3) && digitsOnly3.contains(digitsOnly)) {
                        String str = Tags.get(this, callEntry.number);
                        if (callEntry.name != null) {
                            str = callEntry.name;
                        } else if (str == null) {
                            str = getString(R.string.unknown_caller);
                        }
                        this.dialpadSuggestions.add(new ContactEntry(str, callEntry.number, -1L));
                        hashSet2.add(digitsOnly3);
                        if (this.dialpadSuggestions.size() >= 24) {
                            break;
                        }
                    }
                }
            }
        }
        if (this.dialpadSuggestions.isEmpty()) {
            this.dialpadMatch.setText("");
        } else {
            ContactEntry contactEntry4 = this.dialpadSuggestions.get(0);
            this.matchedName = contactEntry4.name;
            this.matchedNumber = contactEntry4.number;
            this.dialpadMatch.setText(contactEntry4.name + "  •  " + PrivacyUtil.mask(this, contactEntry4.number));
        }
        try { this.dialpadMatch.setVisibility(8); } catch (Exception eMatch) { }
        this.suggestionsAdapter.notifyDataSetChanged();
    }


    public void updateSimBadge() {
        int i;
        TextView textView = this.simBadge;
        if (textView == null) {
            return;
        }
        textView.setVisibility(8);
        if (can("android.permission.READ_PHONE_STATE")) {
            try {
                TelecomManager telecomManager = (TelecomManager) getSystemService("telecom");
                List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
                if (callCapablePhoneAccounts == null || callCapablePhoneAccounts.size() < 2 || (i = getPrefs().getInt("preferred_sim", -1)) == 99) {
                    return;
                }
                if (i < 0 || i >= callCapablePhoneAccounts.size()) {
                    try {
                        PhoneAccountHandle defaultOutgoingPhoneAccount = telecomManager.getDefaultOutgoingPhoneAccount("tel");
                        if (defaultOutgoingPhoneAccount != null) {
                            i = callCapablePhoneAccounts.indexOf(defaultOutgoingPhoneAccount);
                        }
                    } catch (Exception unused) {
                    }
                }
                if (i < 0) {
                    i = 0;
                }
                this.simBadge.setText(String.valueOf(i + 1));
                this.simBadge.setVisibility(0);
            } catch (Exception unused2) {
            }
        }
    }

    private int themeGen;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 77 && i2 == -1) {
            unlockedThisSession = true;
        }
        int i3;
        super.onActivityResult(i, i2, intent);
        if (i == REQ_VOICE && i2 == -1 && intent != null) {
            ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("android.speech.extra.RESULTS");
            if (stringArrayListExtra == null || stringArrayListExtra.isEmpty()) {
                return;
            }
            handleVoiceInput(stringArrayListExtra.get(0));
            return;
        }
        if (i == REQ_PICK_SPEED && i2 == -1 && intent != null && intent.getData() != null && (i3 = this.pendingSpeedSlot) >= 2) {
            this.pendingSpeedSlot = -1;
            saveSpeedDial(intent.getData(), i3);
        } else if (i == REQ_SETTINGS) {
            selectTab(this.currentTab);
            loadHeaderAvatar();
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.currentTab != 0 || this.numberText.getText().length() <= 0) {
            super.onBackPressed();
        } else {
            this.numberText.setText("");
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        setContentView(R.layout.activity_main);
        this.content = (FrameLayout) findViewById(R.id.content);
        LayoutInflater layoutInflater = getLayoutInflater();
        this.favouritesView = layoutInflater.inflate(R.layout.view_favourites, (ViewGroup) this.content, false);
        this.recentsView = layoutInflater.inflate(R.layout.view_recents, (ViewGroup) this.content, false);
        this.contactsView = layoutInflater.inflate(R.layout.view_contacts, (ViewGroup) this.content, false);
        this.dialpadView = layoutInflater.inflate(R.layout.view_dialpad, (ViewGroup) this.content, false);
        this.messagesView = layoutInflater.inflate(R.layout.view_messages, (ViewGroup) this.content, false);
        this.favouritesList = (ListView) this.favouritesView.findViewById(R.id.favouritesList);
        this.recentsList = (ListView) this.recentsView.findViewById(R.id.recentsList);
        this.contactsList = (ListView) this.contactsView.findViewById(R.id.contactsList);
        this.favouritesEmpty = (TextView) this.favouritesView.findViewById(R.id.favouritesEmpty);
        this.recentsEmpty = (TextView) this.recentsView.findViewById(R.id.recentsEmpty);
        this.contactsEmpty = (TextView) this.contactsView.findViewById(R.id.contactsEmpty);
        this.messagesView.findViewById(R.id.chipMsgAll).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.messagesFilter = 0;
                MainActivity.this.tintMessageChips();
                MainActivity.this.loadThreads();
            }
        });
        this.messagesView.findViewById(R.id.chipMsgPersonal).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.messagesFilter = 1;
                MainActivity.this.tintMessageChips();
                MainActivity.this.loadThreads();
            }
        });
        this.messagesView.findViewById(R.id.chipMsgOtp).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.messagesFilter = 2;
                MainActivity.this.tintMessageChips();
                MainActivity.this.loadThreads();
            }
        });
        this.messagesView.findViewById(R.id.chipMsgOthers).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.messagesFilter = 3;
                MainActivity.this.tintMessageChips();
                MainActivity.this.loadThreads();
            }
        });
        tintMessageChips();
        this.messagesView.findViewById(R.id.msgFab).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                final EditText editText = M3Util.input(MainActivity.this, false, "To: phone number");
                editText.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                editText.setMaxLines(1);
                android.widget.LinearLayout msgCard = M3Util.card(MainActivity.this);
                msgCard.addView(editText);
                final android.app.AlertDialog msgDlg = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.new_message).setView(msgCard).setPositiveButton(R.string.msg_send, new DialogInterface.OnClickListener() {
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String trim = editText.getText().toString().trim();
                        if (trim.isEmpty()) {
                            return;
                        }
                        Intent intent = new Intent(MainActivity.this, (Class<?>) MessagesActivity.class);
                        intent.putExtra("thread_id", -1L);
                        intent.putExtra("address", trim);
                        MainActivity.this.startActivity(intent);
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
                msgDlg.show();
                try {
                    msgDlg.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    editText.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) MainActivity.this.getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, 0);
                } catch (Exception e) {
                }
            }
        });
        ((ListView) this.messagesView.findViewById(R.id.threadsList)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                Object[] objArr = (Object[]) adapterView.getItemAtPosition(i);
                Intent intent = new Intent(MainActivity.this, (Class<?>) MessagesActivity.class);
                intent.putExtra("thread_id", (Long) objArr[0]);
                intent.putExtra("address", (String) objArr[1]);
                MainActivity.this.startActivity(intent);
            }
        });
        this.favouritesList.setAdapter((ListAdapter) this.favouritesAdapter);
        this.recentsList.setAdapter((ListAdapter) this.recentsAdapter);
        this.contactsList.setAdapter((ListAdapter) this.contactsAdapter);
        this.tabFavourites = (TextView) findViewById(R.id.tabFavourites);
        this.tabRecents = (TextView) findViewById(R.id.tabRecents);
        this.tabContacts = (TextView) findViewById(R.id.tabContacts);
        this.tabMessages = (TextView) findViewById(R.id.tabMessages);
        this.searchInput = (EditText) findViewById(R.id.searchInput);
        this.numberText = (TextView) this.dialpadView.findViewById(R.id.numberText);
        this.dialpadMatch = (TextView) this.dialpadView.findViewById(R.id.dialpadMatch);
        ((ListView) this.dialpadView.findViewById(R.id.dialpadSuggestions)).setAdapter((ListAdapter) this.suggestionsAdapter);
        buildKeypad();
        this.dialpadView.findViewById(R.id.videoButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String trim = MainActivity.this.numberText.getText().toString().trim();
                if (trim.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.enter_number_first, 0).show();
                    return;
                }
                if (!MainActivity.this.can("android.permission.CALL_PHONE")) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        MainActivity.this.requestPermissions(new String[]{"android.permission.CALL_PHONE"}, 101);
                        return;
                    }
                    return;
                }
                try {
                    String vnum = (MainActivity.this.getPrefs().getBoolean("private_calling", false) && trim != null && !trim.isEmpty()) ? "*31#" + trim : trim;
                    Uri parse = Uri.parse("tel:" + Uri.encode(vnum));
                    Bundle bundle2 = new Bundle();
                    bundle2.putInt("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3);
                    ((TelecomManager) MainActivity.this.getSystemService("telecom")).placeCall(parse, bundle2);
                } catch (Exception unused) {
                    Toast.makeText(MainActivity.this, R.string.video_not_supported, 0).show();
                }
            }
        });
        this.dialpadView.findViewById(R.id.gridButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    View grid = MainActivity.this.dialpadView.findViewById(R.id.keypadGrid);
                    View sug = MainActivity.this.dialpadView.findViewById(R.id.dialpadSuggestions);
                    View match = MainActivity.this.dialpadView.findViewById(R.id.dialpadMatch);
                    View chips = MainActivity.this.recentsView.findViewById(R.id.chipsRow);
                    boolean open = grid.getVisibility() != View.VISIBLE;
                    grid.setVisibility(open ? View.VISIBLE : View.GONE);
                    if (chips != null) {
                        chips.setVisibility(open ? View.GONE : View.VISIBLE);
                    }
                    if (sug != null) {
                        sug.setVisibility(open ? View.VISIBLE : View.GONE);
                    }
                    if (match != null) {
                        match.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                }
            }
        });
        this.dialpadView.findViewById(R.id.waButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String number = MainActivity.this.numberText.getText().toString().trim();
                if (number.isEmpty()) {
                    android.widget.Toast.makeText(MainActivity.this, R.string.type_number_first, 0).show();
                    return;
                }
                try {
                    MainActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + MainActivity.this.international(number))));
                } catch (Exception unused) {
                    android.widget.Toast.makeText(MainActivity.this, R.string.whatsapp_failed, 0).show();
                }
            }
        });
        this.dialpadView.findViewById(R.id.voiceButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
                    intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
                    intent.putExtra("android.speech.extra.PROMPT", MainActivity.this.getString(R.string.voice_prompt));
                    MainActivity.this.startActivityForResult(intent, MainActivity.REQ_VOICE);
                } catch (Exception unused) {
                    Toast.makeText(MainActivity.this, R.string.voice_no, 0).show();
                }
            }
        });


        android.widget.ImageButton quickEye = (android.widget.ImageButton) findViewById(R.id.quickEye);
        if (quickEye != null) {
            quickEye.setImageResource(getPrefs().getBoolean("hide_number", false) ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            quickEye.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    boolean on = !getPrefs().getBoolean("hide_number", false);
                    getPrefs().edit().putBoolean("hide_number", on).commit();
                    ((android.widget.ImageButton) view).setImageResource(on ? R.drawable.ic_eye_off : R.drawable.ic_eye);
                    applyCurrentFilter();
                    loadThreads();
                    Toast.makeText(MainActivity.this, on ? R.string.hide_number : R.string.numbers_visible, 0).show();
                }
            });
        }
        android.widget.ImageButton msgSetIcon = (android.widget.ImageButton) findViewById(R.id.msgSettings);
        if (msgSetIcon != null) {
            msgSetIcon.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, MessageSettingsActivity.class));
                }
            });
        }
        findViewById(R.id.profileHeader).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.startActivityForResult(new Intent(MainActivity.this, (Class<?>) SettingsActivity.class), MainActivity.REQ_SETTINGS);
            }
        });
        loadHeaderAvatar();
        this.headerTitle = (TextView) findViewById(R.id.headerTitle);
        ImageButton imageButton = (ImageButton) findViewById(R.id.addContactButton);
        this.addContactButton = imageButton;
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    Intent intent = new Intent("android.intent.action.INSERT");
                    intent.setType("vnd.android.cursor.dir/raw_contact");
                    MainActivity.this.startActivity(intent);
                } catch (Exception unused) {
                    Toast.makeText(MainActivity.this, "Could not open contacts", 0).show();
                }
            }
        });
        findViewById(R.id.headerSearch).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.searchInput.setVisibility(0);
                MainActivity.this.searchInput.requestFocus();
                if (MainActivity.this.currentTab == 0) {
                    MainActivity.this.selectTab(1);
                }
            }
        });
        ImageButton imageButton2 = (ImageButton) this.dialpadView.findViewById(R.id.deleteButton);
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CharSequence text = MainActivity.this.numberText.getText();
                if (text.length() > 0) {
                    MainActivity.this.numberText.setText(text.subSequence(0, text.length() - 1));
                }
            }
        });
        imageButton2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                MainActivity.this.numberText.setText("");
                return true;
            }
        });
        this.dialpadView.findViewById(R.id.callButton).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                String trim = MainActivity.this.numberText.getText().toString().trim();
                if (trim.isEmpty()) {
                    return;
                }
                MainActivity.this.callNumber(trim);
            }
        });
        this.simBadge = (TextView) this.dialpadView.findViewById(R.id.simBadge);
        this.dialpadView.findViewById(R.id.callButton).setOnLongClickListener(new View.OnLongClickListener() {
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                String trim = MainActivity.this.numberText.getText().toString().trim();
                if (MainActivity.this.can("android.permission.READ_PHONE_STATE")) {
                    try {
                        TelecomManager telecomManager = (TelecomManager) MainActivity.this.getSystemService("telecom");
                        List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
                        if (callCapablePhoneAccounts != null && callCapablePhoneAccounts.size() > 1) {
                            MainActivity mainActivity = MainActivity.this;
                            StringBuilder sb = new StringBuilder("tel:");
                            sb.append(Uri.encode(trim.isEmpty() ? "" : trim));
                            mainActivity.showSimPicker(telecomManager, Uri.parse(sb.toString()), callCapablePhoneAccounts, true);
                            return true;
                        }
                    } catch (Exception unused) {
                    }
                }
                if (!trim.isEmpty()) {
                    MainActivity.this.showNumberOptions(null, trim);
                }
                return true;
            }
        });
        this.numberText.addTextChangedListener(new TextWatcher() {
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                MainActivity.this.updateDialpadMatch();
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
        });
        this.dialpadMatch.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MainActivity.this.matchedName == null || MainActivity.this.matchedNumber == null) {
                    return;
                }
                MainActivity mainActivity = MainActivity.this;
                mainActivity.showNumberOptions(mainActivity.matchedName, MainActivity.this.matchedNumber);
            }
        });
        this.tabFavourites.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.selectTab(3);
            }
        });
        this.tabRecents.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.selectTab(1);
            }
        });
        this.tabContacts.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.selectTab(2);
            }
        });
        this.tabMessages.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.selectTab(4);
            }
        });
        this.recentsView.findViewById(R.id.allChip).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.filterMode = 0;
                MainActivity.this.tintFilterChips();
                MainActivity.this.applyCurrentFilter();
            }
        });
        this.recentsView.findViewById(R.id.missedChip).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.filterMode = 1;
                MainActivity.this.tintFilterChips();
                MainActivity.this.applyCurrentFilter();
            }
        });
        this.recentsView.findViewById(R.id.incomingChip).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.filterMode = 2;
                MainActivity.this.tintFilterChips();
                MainActivity.this.applyCurrentFilter();
            }
        });
        this.recentsView.findViewById(R.id.outgoingChip).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.filterMode = 3;
                MainActivity.this.tintFilterChips();
                MainActivity.this.applyCurrentFilter();
            }
        });
        tintFilterChips();
        this.searchInput.addTextChangedListener(new TextWatcher() {
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                MainActivity.this.applyCurrentFilter();
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList arrayList = new ArrayList();
            if (!can("android.permission.READ_CALL_LOG")) {
                arrayList.add("android.permission.READ_CALL_LOG");
            }
            if (!can("android.permission.WRITE_CALL_LOG")) {
                arrayList.add("android.permission.WRITE_CALL_LOG");
            }
            if (!can("android.permission.READ_CONTACTS")) {
                arrayList.add("android.permission.READ_CONTACTS");
            }
            if (!can("android.permission.READ_PHONE_STATE")) {
                arrayList.add("android.permission.READ_PHONE_STATE");
            }
            if (!can("android.permission.CALL_PHONE")) {
                arrayList.add("android.permission.CALL_PHONE");
            }
            if (Build.VERSION.SDK_INT >= 33 && !can("android.permission.POST_NOTIFICATIONS")) {
                arrayList.add("android.permission.POST_NOTIFICATIONS");
            }
            if (Build.VERSION.SDK_INT >= 33) {
                if (!can("android.permission.READ_MEDIA_AUDIO")) {
                    arrayList.add("android.permission.READ_MEDIA_AUDIO");
                }
            } else if (!can("android.permission.READ_EXTERNAL_STORAGE")) {
                arrayList.add("android.permission.READ_EXTERNAL_STORAGE");
            }
            if (!arrayList.isEmpty()) {
                requestPermissions((String[]) arrayList.toArray(new String[arrayList.size()]), REQ_PERMS);
            }
        }
        int i = getPrefs().getInt("last_tab", 1);
        if (!getPrefs().getBoolean("tabs_v2", false)) {
            getPrefs().edit().putBoolean("tabs_v2", true).commit();
            if (i == 0) {
                i = 3;
            }
        }
        selectTab(i);
        maybeLockApp();
        handleDialIntent();
        maybePromptDefaultDialer();
        this.callLogObserver = new android.database.ContentObserver(this.callLogHandler) {
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MainActivity.this.callLogHandler.removeCallbacksAndMessages(null);
                MainActivity.this.callLogHandler.postDelayed(new Runnable() {
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override // java.lang.Runnable
                                public void run() {
                                    if (MainActivity.this.isFinishing() || MainActivity.this.isDestroyed()) {
                                        return;
                                    }
                                    if (MainActivity.this.can("android.permission.READ_CALL_LOG")) {
                                        MainActivity.this.loadCalls();
                                    }
                                    MainActivity.this.applyCurrentFilter();
                                }
                            });
                        } catch (Exception e) {
                        }
                    }
                }, 800L);
            }
        };
        try {
            getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, this.callLogObserver);
        } catch (Exception e) {
        }
        if (getPrefs().getBoolean("open_dialpad_default", false)) {
            selectTab(0);
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.callLogObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(this.callLogObserver);
            } catch (Exception unused) {
            }
            this.callLogObserver = null;
        }
        ToneGenerator toneGenerator = this.toneGenerator;
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception unused) {
            }
            this.toneGenerator = null;
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        loadFavourites();
        loadCalls();
        loadContacts();
        applyCurrentFilter();
    }

    /* M3 clipboard spam flow: copy a number (e.g. from Truecaller), open the dialer, one tap to mark */
    private void checkClipboardSpam() {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService("clipboard");
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null || cm.getPrimaryClip().getItemCount() == 0) {
                return;
            }
            CharSequence seq = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            String text = seq == null ? "" : seq.toString().trim();
            if (text.isEmpty() || text.length() > 40 || text.equals(getPrefs().getString("last_clip_prompt", ""))) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
            final String dg = sb.toString();
            if (dg.length() < 7 || dg.length() > 15 || SpamUtil.isSpam(this, dg)) {
                return;
            }
            getPrefs().edit().putString("last_clip_prompt", text).commit();
            android.widget.LinearLayout card = M3Util.card(this);
            card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            android.widget.TextView title = new android.widget.TextView(this);
            title.setText(R.string.clip_spam_title);
            title.setTextSize(17.0f);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setTextColor(getResources().getColor(R.color.text_main));
            title.setGravity(android.view.Gravity.CENTER);
            card.addView(title);
            android.widget.TextView numTv = new android.widget.TextView(this);
            numTv.setText(PrivacyUtil.mask(this, dg));
            numTv.setTextSize(20.0f);
            numTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            numTv.setTextColor(getResources().getColor(R.color.text_main));
            numTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams nlp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            nlp.topMargin = M3Util.dp(this, 10);
            card.addView(numTv, nlp);
            android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
            btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = M3Util.dp(this, 20);
            android.widget.TextView cancelBtn = M3Util.outlineBtn(this, getString(R.string.cancel));
            btnRow.addView(cancelBtn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView spamBtn = M3Util.greenBtn(this, getString(R.string.mark_spam_full), 0);
            android.widget.LinearLayout.LayoutParams slp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            slp.leftMargin = M3Util.dp(this, 10);
            btnRow.addView(spamBtn, slp);
            card.addView(btnRow, blp);
            final android.app.AlertDialog dlg = new AlertDialog.Builder(this).setView(card).create();
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                }
            });
            spamBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SpamUtil.add(MainActivity.this, dg);
                    Tags.set(MainActivity.this, dg, "Spam");
                    try { dlg.dismiss(); } catch (Exception e) { }
                    Toast.makeText(MainActivity.this, R.string.spam_saved, 0).show();
                }
            });
            M3Util.scrim(dlg);
        } catch (Exception e) {
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
        if (CallState.justEnded) {
            CallState.justEnded = false;
            if (this.currentTab == 0) {
                this.numberText.setText("");
            }
            showAfterCall();
        }
        boolean z = getPrefs().getBoolean("show_favourites", true);
        this.tabFavourites.setVisibility(z ? 0 : 8);
        if (this.currentTab == 3 && !z) {
            selectTab(1);
        }
        updateSimBadge();
        if (can("android.permission.READ_CONTACTS")) {
            loadContacts();
            updateDialpadMatch();
            applyCurrentFilter();
        }
        SpamUtil.seed(this);
        checkClipboardSpam();
    }
}
