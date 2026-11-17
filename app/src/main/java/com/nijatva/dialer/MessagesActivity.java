package com.nijatva.dialer;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class MessagesActivity extends Activity {
    private BaseAdapter adapter;
    private String address;
    private ListView list;
    private final ArrayList<Object[]> msgs = new ArrayList<>();
    private long threadId;

    /* loaded from: classes.dex */
    private class MsgsAdapter extends BaseAdapter {
        private MsgsAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return MessagesActivity.this.msgs.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return MessagesActivity.this.msgs.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getItemViewType(int i) {
            return !Boolean.TRUE.equals(((Object[]) MessagesActivity.this.msgs.get(i))[2]) ? 1 : 0;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            Object[] objArr = (Object[]) MessagesActivity.this.msgs.get(i);
            if (view == null) {
                view = MessagesActivity.this.getLayoutInflater().inflate(getItemViewType(i) == 0 ? R.layout.item_msg_sent : R.layout.item_msg_received, viewGroup, false);
            }
            ((TextView) view.findViewById(R.id.msgText)).setText((String) objArr[0]);
            return view;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getViewTypeCount() {
            return 2;
        }
    }

    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private void loadMsgs() {
        if (Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_SMS") == 0) {
            new Thread(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    String str;
                    String[] strArr;
                    final ArrayList arrayList = new ArrayList();
                    try {
                        if (MessagesActivity.this.threadId > 0) {
                            str = "thread_id=?";
                            strArr = new String[]{String.valueOf(MessagesActivity.this.threadId)};
                        } else {
                            str = "address=?";
                            strArr = new String[1];
                            strArr[0] = MessagesActivity.this.address == null ? "" : MessagesActivity.this.address;
                        }
                        Cursor query = MessagesActivity.this.getContentResolver().query(Uri.parse("content://sms"), new String[]{"body", "date", "type", "thread_id"}, str, strArr, "date ASC");
                        if (query != null) {
                            while (query.moveToNext()) {
                                MessagesActivity.this.threadId = query.getLong(3);
                                int i = query.getInt(2);
                                if (i == 1 || i == 2) {
                                    Object[] objArr = new Object[3];
                                    objArr[0] = query.getString(0);
                                    objArr[1] = Long.valueOf(query.getLong(1));
                                    objArr[2] = Boolean.valueOf(i == 2);
                                    arrayList.add(objArr);
                                }
                            }
                            query.close();
                        }
                    } catch (Exception unused) {
                    }
                    MessagesActivity.this.runOnUiThread(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            MessagesActivity.this.msgs.clear();
                            MessagesActivity.this.msgs.addAll(arrayList);
                            MessagesActivity.this.adapter.notifyDataSetChanged();
                            MessagesActivity.this.list.setSelection(Math.max(MessagesActivity.this.msgs.size() - 1, 0));
                        }
                    });
                }
            }).start();
        } else {
            requestPermissions(new String[]{"android.permission.READ_SMS"}, 105);
        }
    }

    private String resolveName(String str) {
        if (str == null) {
            return null;
        }
        try {
            Cursor query = getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"display_name"}, null, null, null);
            if (query != null) {
                String string = query.moveToFirst() ? query.getString(0) : null;
                query.close();
                return string;
            }
        } catch (Exception unused) {
        }
        return null;
    }


    public void sendSms(EditText editText) {
        String trim = editText.getText().toString().trim();
        if (trim.isEmpty() || this.address == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.SEND_SMS") != 0) {
            requestPermissions(new String[]{"android.permission.SEND_SMS"}, 106);
            return;
        }
        try {
            android.app.PendingIntent dlrPi = null;
            if (getSharedPreferences("mydialer", 0).getBoolean("delivery_reports", false)) {
                /* FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE - required on Android 12+ */
                int dlrFlags = Build.VERSION.SDK_INT >= 23 ? 201326592 : 134217728;
                dlrPi = android.app.PendingIntent.getBroadcast(this, 0, new Intent("com.nijatva.dialer.SMS_DELIVERED"), dlrFlags);
            }
            SmsManager.getDefault().sendTextMessage(this.address, null, trim, null, dlrPi);
            if (isDefaultSmsApp()) {
                try {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("address", this.address);
                    contentValues.put("body", trim);
                    contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
                    contentValues.put("read", (Integer) 1);
                    contentValues.put("type", (Integer) 2);
                    long j = this.threadId;
                    if (j > 0) {
                        contentValues.put("thread_id", Long.valueOf(j));
                    }
                    getContentResolver().insert(Uri.parse("content://sms/sent"), contentValues);
                } catch (Exception unused) {
                }
                loadMsgs();
            } else {
                this.msgs.add(new Object[]{trim, Long.valueOf(System.currentTimeMillis()), Boolean.TRUE});
                this.adapter.notifyDataSetChanged();
                this.list.setSelection(this.msgs.size() - 1);
                if (!getPackageName().equals(android.provider.Telephony.Sms.getDefaultSmsPackage(this))) {
                    Toast.makeText(this, R.string.msg_hint_default, 1).show();
                }
            }
            editText.setText("");
            Toast.makeText(this, R.string.msg_sent, 0).show();
        } catch (Exception unused2) {
            Toast.makeText(this, R.string.msg_send_failed, 0).show();
        }
    }

    private final android.content.BroadcastReceiver dlrReceiver = new android.content.BroadcastReceiver() {
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MessagesActivity.this, getResultCode() == -1 ? R.string.sms_delivered_toast : R.string.sms_not_delivered_toast, 0).show();
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        String str;
        String schemeSpecificPart;
        super.onCreate(bundle);
        setContentView(R.layout.activity_messages);
        try {
            android.content.IntentFilter dlrFilter = new android.content.IntentFilter("com.nijatva.dialer.SMS_DELIVERED");
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(this.dlrReceiver, dlrFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(this.dlrReceiver, dlrFilter);
            }
        } catch (Exception e) {
        }
        this.threadId = getIntent().getLongExtra("thread_id", -1L);
        this.address = getIntent().getStringExtra("address");
        Uri data = getIntent().getData();
        if (data != null) {
            String scheme = data.getScheme() == null ? "" : data.getScheme();
            if (("sms".equals(scheme) || "smsto".equals(scheme)) && (schemeSpecificPart = data.getSchemeSpecificPart()) != null) {
                int indexOf = schemeSpecificPart.indexOf(63);
                if (indexOf >= 0) {
                    schemeSpecificPart = schemeSpecificPart.substring(0, indexOf);
                }
                if (!schemeSpecificPart.trim().isEmpty()) {
                    this.address = schemeSpecificPart.trim();
                }
            }
        }
        String resolveName = resolveName(this.address);
        TextView textView = (TextView) findViewById(R.id.msgName);
        if (resolveName != null) {
            str = resolveName;
        } else {
            str = PrivacyUtil.mask(this, this.address);
            if (str == null || str.isEmpty()) {
                str = "?";
            }
        }
        textView.setText(str);
        ((TextView) findViewById(R.id.msgNumber)).setText(resolveName != null ? PrivacyUtil.mask(this, this.address) : "");
        findViewById(R.id.msgBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MessagesActivity.this.finish();
            }
        });
        this.list = (ListView) findViewById(R.id.msgList);
        MsgsAdapter msgsAdapter = new MsgsAdapter();
        this.adapter = msgsAdapter;
        this.list.setAdapter((ListAdapter) msgsAdapter);
        final EditText editText = (EditText) findViewById(R.id.msgInput);
        findViewById(R.id.msgSend).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MessagesActivity.this.sendSms(editText);
            }
        });
        loadMsgs();
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 105 || i == 106) {
            loadMsgs();
        }
    }
    private int themeGen;

    @Override // android.view.ContextThemeWrapper
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        try {
            unregisterReceiver(this.dlrReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }
}
