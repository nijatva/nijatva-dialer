package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* Detects existing call recordings on storage and plays them in an M3 mini player sheet */
public class RecordingsUtil {
    public static class RecEntry {
        public final Uri uri;
        public final String name;
        public final long duration;
        public final long date;

        RecEntry(Uri uri, String name, long duration, long date) {
            this.uri = uri;
            this.name = name;
            this.duration = duration;
            this.date = date;
        }
    }

    private static volatile List<RecEntry> cache = new ArrayList<>();
    private static volatile long cacheAt = 0;
    private static volatile boolean scanning = false;

    /* Kick off a background scan if the cache is stale (30 s) */
    public static void ensureScan(Context ctx) {
        if (scanning || System.currentTimeMillis() - cacheAt < 30000L) {
            return;
        }
        scanning = true;
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                List<RecEntry> out = new ArrayList<>();
                try {
                    scanMediaStore(app, out);
                } catch (Exception e) {
                }
                try {
                    scanLegacyDirs(out);
                } catch (Exception e) {
                }
                cache = out;
                cacheAt = System.currentTimeMillis();
                scanning = false;
            }
        }).start();
    }

    private static void scanMediaStore(Context ctx, List<RecEntry> out) {
        try {
            android.database.Cursor c = ctx.getContentResolver().query(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    new String[]{"_id", "_display_name", "duration", "date_modified"},
                    "mime_type LIKE 'audio/%'", null, "date_modified DESC");
            if (c == null) {
                return;
            }
            int count = 0;
            while (c.moveToNext() && count < 1500) {
                String name = c.getString(1);
                if (name == null) {
                    continue;
                }
                String low = name.toLowerCase(Locale.US);
                boolean keep = low.contains("record") || low.contains("call") || hasLongDigits(name);
                if (!keep) {
                    continue;
                }
                long dur = 0;
                try {
                    dur = c.getLong(2);
                } catch (Exception e) {
                    dur = 0;
                }
                long dt = 0;
                try {
                    dt = c.getLong(3);
                } catch (Exception e) {
                    dt = 0;
                }
                out.add(new RecEntry(android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Files.getContentUri("external"), c.getLong(0)), name, dur, dt * 1000L));
                count++;
            }
            c.close();
        } catch (Exception e) {
        }
    }

    private static boolean hasLongDigits(String s) {
        int run = 0;
        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) {
                run++;
                if (run >= 7) {
                    return true;
                }
            } else {
                run = 0;
            }
        }
        return false;
    }

    private static void scanLegacyDirs(List<RecEntry> out) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            String[] dirs = {"CallRecordings", "Recordings", "Recordings/Call", "Music/Recordings", "Music/Recordings/Call", "Music/CallRecordings", "CallRecord"};
            for (String d : dirs) {
                listRec(new File(root, d), out);
            }
        } catch (Exception e) {
        }
    }

    private static void listRec(File dir, List<RecEntry> out) {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return;
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                listRec(f, out);
            } else {
                String n = f.getName().toLowerCase(Locale.US);
                if (n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".amr") || n.endsWith(".3gp") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".aac")) {
                    out.add(new RecEntry(Uri.fromFile(f), f.getName(), 0, f.lastModified()));
                }
            }
        }
    }

    /* Does this recording file belong to this phone number? */
    private static boolean matches(String filename, String number) {
        String nd = SpamUtil.digits(number);
        if (nd.length() < 7) {
            return false;
        }
        String tail = nd.length() > 10 ? nd.substring(nd.length() - 10) : nd;
        return SpamUtil.digits(filename).contains(tail);
    }

    public static RecEntry firstRecording(Context ctx, String number) {
        ensureScan(ctx);
        if (number == null) {
            return null;
        }
        for (RecEntry r : cache) {
            if (matches(r.name, number)) {
                return r;
            }
        }
        return null;
    }

    public static boolean hasRecording(Context ctx, String number) {
        return firstRecording(ctx, number) != null;
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return s / 60 + ":" + String.format(Locale.US, "%02d", s % 60);
    }

    /* M3 bottom mini-player sheet: play/pause, seekbar and waveform bars */
    public static void showPlayerSheet(final Activity act, final RecEntry rec) {
        try {
            final MediaPlayer[] mp = {null};
            final Handler h = new Handler();
            LinearLayout card = M3Util.card(act);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView title = new TextView(act);
            title.setText(rec.name);
            title.setTextSize(15.0f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(act.getResources().getColor(R.color.text_main));
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            card.addView(title);
            final TextView time = new TextView(act);
            time.setText("0:00 / --:--");
            time.setTextSize(12.0f);
            time.setTextColor(act.getResources().getColor(R.color.text_hint));
            time.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
            tlp.topMargin = M3Util.dp(act, 8);
            card.addView(time, tlp);
            final SeekBar seek = new SeekBar(act);
            seek.setProgressTintList(ColorStateList.valueOf(0xff2fbf4f));
            seek.setThumbTintList(ColorStateList.valueOf(0xff2fbf4f));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
            slp.topMargin = M3Util.dp(act, 6);
            card.addView(seek, slp);
            final LinearLayout wave = new LinearLayout(act);
            wave.setOrientation(LinearLayout.HORIZONTAL);
            wave.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, M3Util.dp(act, 34));
            wlp.topMargin = M3Util.dp(act, 14);
            card.addView(wave, wlp);
            final List<View> bars = new ArrayList<>();
            long seed = Math.abs(rec.name.toLowerCase(Locale.US).hashCode());
            for (int i = 0; i < 36; i++) {
                View bar = new View(act);
                int hgt = M3Util.dp(act, (int) (10 + ((seed >> (i % 28)) & 15) + (i % 3) * 4));
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(M3Util.dp(act, 3), hgt);
                blp.leftMargin = M3Util.dp(act, 1);
                blp.rightMargin = M3Util.dp(act, 1);
                bar.setBackgroundColor(0xff3d3d3d);
                wave.addView(bar, blp);
                bars.add(bar);
            }
            LinearLayout btnRow = new LinearLayout(act);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = M3Util.dp(act, 18);
            final TextView playBtn = M3Util.greenBtn(act, "PLAY", 0);
            btnRow.addView(playBtn, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView closeBtn = M3Util.outlineBtn(act, act.getString(R.string.crash_close));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            clp.leftMargin = M3Util.dp(act, 10);
            btnRow.addView(closeBtn, clp);
            card.addView(btnRow, rlp);
            final AlertDialog dlg = new AlertDialog.Builder(act).setView(card).create();
            final Runnable[] ticker = new Runnable[1];
            ticker[0] = new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (mp[0] != null) {
                            int pos = mp[0].getCurrentPosition();
                            int dur = mp[0].getDuration();
                            if (dur > 0) {
                                time.setText(fmt(pos) + " / " + fmt(dur));
                                seek.setMax(dur);
                                seek.setProgress(pos);
                                int lit = (int) (bars.size() * (pos / (float) dur));
                                for (int i = 0; i < bars.size(); i++) {
                                    bars.get(i).setBackgroundColor(i <= lit ? 0xff2fbf4f : 0xff3d3d3d);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                    h.postDelayed(ticker[0], 200L);
                }
            };
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mp[0] != null) {
                        try {
                            mp[0].seekTo(progress);
                        } catch (Exception e) {
                        }
                    }
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override // android.widget.SeekBar.OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        if (mp[0] == null) {
                            mp[0] = new MediaPlayer();
                            mp[0].setDataSource(act, rec.uri);
                            mp[0].prepare();
                            seek.setMax(mp[0].getDuration());
                            mp[0].start();
                            playBtn.setText("PAUSE");
                            h.post(ticker[0]);
                        } else if (mp[0].isPlaying()) {
                            mp[0].pause();
                            playBtn.setText("PLAY");
                        } else {
                            mp[0].start();
                            playBtn.setText("PAUSE");
                        }
                    } catch (Exception e) {
                        Toast.makeText(act, R.string.rec_error, 0).show();
                    }
                }
            });
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try { dlg.dismiss(); } catch (Exception e) { }
                }
            });
            dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override // android.content.DialogInterface.OnDismissListener
                public void onDismiss(android.content.DialogInterface dialogInterface) {
                    h.removeCallbacksAndMessages(null);
                    if (mp[0] != null) {
                        try {
                            mp[0].release();
                        } catch (Exception e) {
                        }
                        mp[0] = null;
                    }
                }
            });
            M3Util.scrim(dlg);
            try {
                dlg.getWindow().setGravity(Gravity.BOTTOM);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            Toast.makeText(act, R.string.rec_error, 0).show();
        }
    }
}
