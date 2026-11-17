package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/* loaded from: classes.dex */
public class SpeedDialActivity extends Activity {
    private static final int REQ_PICK = 500;
    private SpeedAdapter adapter;
    private int pendingSlot = -1;


    /* loaded from: classes.dex */
    public class SpeedAdapter extends BaseAdapter {
        private SpeedAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return 8;
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return Integer.valueOf(i + 2);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = SpeedDialActivity.this.getLayoutInflater().inflate(R.layout.item_speed_dial, viewGroup, false);
            }
            final int i2 = i + 2;
            TextView keyView = (TextView) view.findViewById(R.id.speedKey);
            String string = SpeedDialActivity.this.getPrefs().getString("speed" + i2, null);
            TextView textView = (TextView) view.findViewById(R.id.speedName);
            TextView textView2 = (TextView) view.findViewById(R.id.speedNumber);
            if (string == null || !string.contains("|")) {
                keyView.setText("+");
                keyView.setBackgroundResource(R.drawable.bg_badge_gray);
                keyView.setTextColor(SpeedDialActivity.this.getResources().getColor(R.color.text_hint));
                textView.setText(R.string.speed_not_set);
                textView.setTextColor(SpeedDialActivity.this.getResources().getColor(R.color.text_hint));
                textView2.setText("");
            } else {
                keyView.setText(String.valueOf(i2));
                keyView.setBackgroundResource(R.drawable.bg_badge_circle);
                keyView.setTextColor(-1);
                textView.setText(string.substring(0, string.indexOf(124)));
                textView.setTextColor(SpeedDialActivity.this.getResources().getColor(R.color.text_main));
                textView2.setText(string.substring(string.indexOf(124) + 1));
            }
            View removeBtn = view.findViewById(R.id.speedRemove);
            if (removeBtn != null) {
                removeBtn.setVisibility((string != null && string.contains("|")) ? 0 : 8);
                final String removedVal = string;
                removeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View v) {
                        SpeedDialActivity.this.getPrefs().edit().remove("speed" + i2).commit();
                        SpeedDialActivity.this.adapter.notifyDataSetChanged();
                        SpeedDialActivity.this.showUndoSheet(i2, removedVal);
                    }
                });
            }
            View.OnClickListener slotClick = new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    String string2 = SpeedDialActivity.this.getPrefs().getString("speed" + i2, null);
                    if (string2 == null || !string2.contains("|")) {
                        SpeedDialActivity.this.pickContact(i2);
                    } else {
                        SpeedDialActivity.this.showSlotDialog(i2, string2.substring(0, string2.indexOf(124)), string2.substring(string2.indexOf(124) + 1));
                    }
                }
            };
            view.setOnClickListener(slotClick);
            View speedRow = view.findViewById(R.id.speedRow);
            if (speedRow != null) {
                speedRow.setOnClickListener(slotClick);
            }
            return view;
        }
    }


    public SharedPreferences getPrefs() {
        return getSharedPreferences("mydialer", 0);
    }


    public void pickContact(final int i) {
        this.pendingSlot = i;
        final java.util.ArrayList<String> names = new java.util.ArrayList<>();
        final java.util.ArrayList<String> numbers = new java.util.ArrayList<>();
        final java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        final java.util.HashSet<String> seen = new java.util.HashSet<>();
        try {
            Cursor query = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "data1", "contact_id"}, null, null, "display_name ASC");
            if (query != null) {
                while (query.moveToNext()) {
                    String nm = query.getString(0);
                    String num = query.getString(1);
                    if (num == null || num.trim().isEmpty()) {
                        continue;
                    }
                    /* v143: fallback - if DISPLAY_NAME is null/empty, title shows the phone number */
                    if (nm == null || nm.trim().isEmpty()) {
                        nm = num;
                    }
                    String key = num.replaceAll("\\D", "");
                    if (!key.isEmpty() && !seen.add(key)) {
                        continue;
                    }
                    names.add(nm);
                    numbers.add(num);
                    ids.add(Long.valueOf(query.getLong(2)));
                }
                query.close();
            }
        } catch (Exception unused) {
        }
        if (names.isEmpty()) {
            Toast.makeText(this, "Could not open contacts", 0).show();
            return;
        }
        android.widget.LinearLayout card = M3Util.card(this);
        final android.widget.EditText search = M3Util.input(this, false, getString(R.string.search_hint));
        search.setSingleLine(true);
        card.addView(search);
        final java.util.ArrayList<String> fn = new java.util.ArrayList<>(names);
        final java.util.ArrayList<String> fnum = new java.util.ArrayList<>(numbers);
        final java.util.ArrayList<Long> fid = new java.util.ArrayList<>(ids);
        final java.util.HashMap<Long, android.graphics.Bitmap> photos = new java.util.HashMap<>();
        final android.widget.ListView list = new android.widget.ListView(this);
        list.setDivider(null);
        list.setAdapter(new android.widget.BaseAdapter() {
            @Override // android.widget.Adapter
            public int getCount() {
                return fn.size();
            }

            @Override // android.widget.Adapter
            public Object getItem(int pos) {
                return fn.get(pos);
            }

            @Override // android.widget.Adapter
            public long getItemId(int pos) {
                return (long) pos;
            }

            private android.graphics.Bitmap photo(long cid) {
                Long k = Long.valueOf(cid);
                if (photos.containsKey(k)) {
                    return photos.get(k);
                }
                android.graphics.Bitmap b = null;
                try {
                    android.net.Uri u = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cid);
                    java.io.InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), u, true);
                    if (is != null) {
                        b = android.graphics.BitmapFactory.decodeStream(is);
                        try { is.close(); } catch (Exception e) { }
                    }
                } catch (Exception e) {
                }
                photos.put(k, b);
                return b;
            }

            @Override // android.widget.Adapter
            public View getView(int pos, View convertView, android.view.ViewGroup parent) {
                android.widget.LinearLayout row = convertView instanceof android.widget.LinearLayout ? (android.widget.LinearLayout) convertView : new android.widget.LinearLayout(SpeedDialActivity.this);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                int px = M3Util.dp(SpeedDialActivity.this, 10);
                row.setPadding(px, px, px, px);
                row.removeAllViews();
                android.widget.ImageView av = new android.widget.ImageView(SpeedDialActivity.this);
                android.graphics.Bitmap b = photo(fid.get(pos).longValue());
                if (b != null) {
                    av.setImageBitmap(b);
                } else {
                    av.setImageResource(R.drawable.ic_account);
                    av.setColorFilter(getResources().getColor(R.color.icon_gray));
                }
                row.addView(av, new android.widget.LinearLayout.LayoutParams(M3Util.dp(SpeedDialActivity.this, 42), M3Util.dp(SpeedDialActivity.this, 42)));
                android.widget.LinearLayout txt = new android.widget.LinearLayout(SpeedDialActivity.this);
                txt.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.TextView nm = new android.widget.TextView(SpeedDialActivity.this);
                nm.setText(fn.get(pos));
                nm.setTextSize(15.0f);
                nm.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                nm.setTextColor(getResources().getColor(R.color.text_main));
                txt.addView(nm);
                /* v143: secondary line shows the number only when title is a real name */
                if (!fn.get(pos).equals(fnum.get(pos))) {
                    android.widget.TextView num = new android.widget.TextView(SpeedDialActivity.this);
                    num.setText(fnum.get(pos));
                    num.setTextSize(12.0f);
                    num.setTextColor(getResources().getColor(R.color.text_hint));
                    txt.addView(num);
                }
                android.widget.LinearLayout.LayoutParams tp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
                tp.leftMargin = M3Util.dp(SpeedDialActivity.this, 12);
                row.addView(txt, tp);
                return row;
            }
        });
        card.addView(list, new android.widget.LinearLayout.LayoutParams(-1, M3Util.dp(this, 400)));
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                String q = charSequence.toString().trim().toLowerCase();
                fn.clear();
                fnum.clear();
                fid.clear();
                for (int j2 = 0; j2 < names.size(); j2++) {
                    if (q.isEmpty() || names.get(j2).toLowerCase().contains(q) || numbers.get(j2).replace(" ", "").contains(q.replace(" ", ""))) {
                        fn.add(names.get(j2));
                        fnum.add(numbers.get(j2));
                        fid.add(ids.get(j2));
                    }
                }
                ((android.widget.BaseAdapter) list.getAdapter()).notifyDataSetChanged();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(android.text.Editable editable) {
            }
        });
        final AlertDialog pickDlg = new AlertDialog.Builder(this).setTitle(R.string.pick_contact).setView(card).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(android.widget.AdapterView<?> adapterView, View view, int i2, long l) {
                if (i2 < fnum.size()) {
                    SpeedDialActivity.this.getPrefs().edit().putString("speed" + i, fn.get(i2) + "|" + fnum.get(i2)).commit();
                    Toast.makeText(SpeedDialActivity.this, R.string.speed_dial_set, 0).show();
                    SpeedDialActivity.this.adapter.notifyDataSetChanged();
                }
                try {
                    pickDlg.dismiss();
                } catch (Exception e) {
                }
            }
        });
        pickDlg.show();
    }

    /* Bottom undo sheet after removing a speed dial contact */
    private void showUndoSheet(final int i, final String value) {
        try {
            android.widget.LinearLayout box = new android.widget.LinearLayout(this);
            box.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            box.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = (int) (getResources().getDisplayMetrics().density * 20.0f);
            box.setPadding(pad, pad / 2, pad, pad / 2);
            box.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_settings_box));
            android.widget.TextView msg = new android.widget.TextView(this);
            msg.setText(R.string.speed_removed_undo);
            msg.setTextSize(14.0f);
            msg.setTextColor(getResources().getColor(R.color.text_main));
            android.widget.LinearLayout.LayoutParams mp = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            box.addView(msg, mp);
            android.widget.TextView undo = new android.widget.TextView(this);
            undo.setText(R.string.undo_label);
            undo.setTextSize(14.0f);
            undo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            undo.setTextColor(getResources().getColor(R.color.brand_green));
            undo.setPadding(pad / 2, 0, 0, 0);
            box.addView(undo);
            final AlertDialog d = new AlertDialog.Builder(this).setView(box).create();
            undo.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SpeedDialActivity.this.getPrefs().edit().putString("speed" + i, value).commit();
                    SpeedDialActivity.this.adapter.notifyDataSetChanged();
                    try {
                        d.dismiss();
                    } catch (Exception e) {
                    }
                }
            });
            d.show();
            try {
                android.view.Window w = d.getWindow();
                if (w != null) {
                    w.setGravity(android.view.Gravity.BOTTOM);
                    w.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
                    w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                }
            } catch (Exception e2) {
            }
            new android.os.Handler().postDelayed(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        d.dismiss();
                    } catch (Exception e) {
                    }
                }
            }, 4000L);
        } catch (Exception e3) {
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
                        this.adapter.notifyDataSetChanged();
                    }
                }
                query.close();
            }
        } catch (Exception unused) {
        }
    }


    public void showSlotDialog(final int i, String str, String str2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(str + "  (" + str2 + ")");
        builder.setItems(new CharSequence[]{getString(R.string.change), getString(R.string.remove)}, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                if (i2 == 0) {
                    SpeedDialActivity.this.pickContact(i);
                    return;
                }
                SpeedDialActivity.this.getPrefs().edit().remove("speed" + i).commit();
                Toast.makeText(SpeedDialActivity.this, R.string.speed_dial_removed, 0).show();
                SpeedDialActivity.this.adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
        builder.show();
    }

    private int themeGen;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        int i3;
        super.onActivityResult(i, i2, intent);
        if (i != REQ_PICK || i2 != -1 || intent == null || intent.getData() == null || (i3 = this.pendingSlot) < 2) {
            return;
        }
        this.pendingSlot = -1;
        saveSpeedDial(intent.getData(), i3);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_speed_dial);
        findViewById(R.id.speedBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SpeedDialActivity.this.finish();
            }
        });
        this.adapter = new SpeedAdapter();
        ((ListView) findViewById(R.id.speedList)).setAdapter((ListAdapter) this.adapter);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
        this.adapter.notifyDataSetChanged();
    }
}
