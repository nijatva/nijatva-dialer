package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class BlockedListActivity extends Activity {
    private BlockedAdapter adapter;
    private final List<String> blocked = new ArrayList();


    /* loaded from: classes.dex */
    public class BlockedAdapter extends BaseAdapter {
        private BlockedAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return BlockedListActivity.this.blocked.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return BlockedListActivity.this.blocked.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = BlockedListActivity.this.getLayoutInflater().inflate(R.layout.item_blocked, viewGroup, false);
            }
            final String str = (String) BlockedListActivity.this.blocked.get(i);
            ((TextView) view.findViewById(R.id.blockedNumber)).setText(str);
            view.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    BlockedListActivity.this.unblock(str);
                }
            });
            return view;
        }
    }


    public void reload() {
        this.blocked.clear();
        Set<String> stringSet = getSharedPreferences("mydialer", 0).getStringSet("blocked", null);
        if (stringSet != null) {
            this.blocked.addAll(new HashSet(stringSet));
        }
        Collections.sort(this.blocked);
        this.adapter.notifyDataSetChanged();
        findViewById(R.id.blockedEmpty).setVisibility(this.blocked.isEmpty() ? 0 : 8);
        findViewById(R.id.blockedList).setVisibility(this.blocked.isEmpty() ? 8 : 0);
    }


    public void unblock(final String str) {
        new AlertDialog.Builder(this).setTitle(str).setMessage(R.string.unblock_q).setPositiveButton(R.string.unblock_number, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences sharedPreferences = BlockedListActivity.this.getSharedPreferences("mydialer", 0);
                Set<String> stringSet = sharedPreferences.getStringSet("blocked", null);
                HashSet hashSet = stringSet == null ? new HashSet() : new HashSet(stringSet);
                hashSet.remove(str);
                sharedPreferences.edit().putStringSet("blocked", hashSet).commit();
                BlockedListActivity.this.reload();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    private int themeGen;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_blocked);
        findViewById(R.id.blockedBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                BlockedListActivity.this.finish();
            }
        });
        this.adapter = new BlockedAdapter();
        ((ListView) findViewById(R.id.blockedList)).setAdapter((ListAdapter) this.adapter);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
        reload();
    }
}
