package com.nijatva.dialer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes.dex */
public class CallOverlayService extends Service {
    public static final String EXTRA_NUMBER = "extra_number";
    public static final String EXTRA_NAME = "extra_name";
    private static final String CHANNEL_ID = "call_overlay_channel";
    private WindowManager windowManager;
    private View overlayView;

    private static String digits(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String international(String str) {
        String digits = digits(str);
        return digits.length() == 10 ? "91" + digits : digits;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Call Overlay", 2));
            }
        }
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                startForeground(1001, buildNotification());
            } catch (Exception unused) {
            }
        }
        if (intent != null && intent.getBooleanExtra("ended", false)) {
            if (this.overlayView == null) {
                stopSelf();
                return 2;
            }
            try {
                ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(R.string.call_ended);
            } catch (Exception unused2) {
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    CallOverlayService.this.stopSelf();
                }
            }, 10000L);
            return 2;
        }
        String str = null;
        String str2 = null;
        if (intent != null) {
            str = intent.getStringExtra(EXTRA_NUMBER);
            str2 = intent.getStringExtra(EXTRA_NAME);
        }
        showOverlay(str2, str);
        return 2;
    }

    private Notification buildNotification() {
        Notification.Builder title;
        if (Build.VERSION.SDK_INT >= 26) {
            title = new Notification.Builder(this, CHANNEL_ID);
        } else {
            title = new Notification.Builder(this);
        }
        return title.setContentTitle(getString(R.string.app_name)).setSmallIcon(R.drawable.ic_call).setPriority(1).build();
    }

    private void showOverlay(String str, String str2) {
        if (this.overlayView != null || str2 == null) {
            return;
        }
        try {
            String tag = Tags.get(this, str2);
            String display = str != null ? str : (tag != null ? tag : getString(R.string.unknown_caller));
            this.windowManager = (WindowManager) getSystemService("window");
            this.overlayView = LayoutInflater.from(ThemeUtil.wrap(this)).inflate(R.layout.overlay_call_popup, null);
            String callerEmoji = EmojiUtil.display(this, str2);
            ((TextView) this.overlayView.findViewById(R.id.overlay_name)).setText(callerEmoji.isEmpty() ? display : callerEmoji + " " + display);
            String catOv = SpamUtil.getCategory(this, str2);
            if (catOv != null) {
                ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(catOv);
            } else {
                String susOv = SpamUtil.getSuspected(this, str2);
                if (susOv != null) {
                    ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(susOv);
                }
            }
            ((TextView) this.overlayView.findViewById(R.id.overlay_number)).setText(str2);
            String substring = (display == null || display.isEmpty()) ? "#" : display.substring(0, 1).toUpperCase(Locale.US);
            ((TextView) this.overlayView.findViewById(R.id.overlay_avatar)).setText(substring);
            ((TextView) this.overlayView.findViewById(R.id.overlay_time)).setText(new SimpleDateFormat("hh:mm a", Locale.US).format(new Date()));
            if (Tags.isSpam(this, str2)) {
                ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(R.string.mark_spam);
            } else if (str == null && tag != null) {
                ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(R.string.from_contacts);
            } else if (str == null) {
                ((TextView) this.overlayView.findViewById(R.id.overlay_source)).setText(R.string.unknown_caller);
            }
            final String str3 = str2;
            this.overlayView.findViewById(R.id.overlay_close).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    CallOverlayService.this.stopSelf();
                }
            });
            this.overlayView.findViewById(R.id.overlayCall).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        CallOverlayService.this.startActivity(new Intent("android.intent.action.DIAL", Uri.parse("tel:" + Uri.encode(str3))).addFlags(268435456));
                    } catch (Exception unused) {
                    }
                    CallOverlayService.this.stopSelf();
                }
            });
            this.overlayView.findViewById(R.id.overlayMsg).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        CallOverlayService.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://wa.me/" + CallOverlayService.this.international(str3))).addFlags(268435456));
                    } catch (Exception unused) {
                    }
                    CallOverlayService.this.stopSelf();
                }
            });
            this.overlayView.findViewById(R.id.overlaySpam).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    Tags.set(CallOverlayService.this, str3, "Spam");
                    SpamUtil.add(CallOverlayService.this, str3);
                    Toast.makeText(CallOverlayService.this, R.string.spam_saved, 0).show();
                    CallOverlayService.this.stopSelf();
                }
            });
            this.overlayView.findViewById(R.id.overlayBlock).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SharedPreferences sharedPreferences = CallOverlayService.this.getSharedPreferences("mydialer", 0);
                    Set<String> stringSet = sharedPreferences.getStringSet("blocked", null);
                    HashSet hashSet = stringSet == null ? new HashSet() : new HashSet(stringSet);
                    hashSet.add(digits(str3));
                    sharedPreferences.edit().putStringSet("blocked", hashSet).commit();
                    Toast.makeText(CallOverlayService.this, R.string.blocked_toast, 0).show();
                    CallOverlayService.this.stopSelf();
                }
            });
            this.overlayView.findViewById(R.id.overlayProfile).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(CallOverlayService.this, CallDetailActivity.class);
                        intent.putExtra("number", str3);
                        intent.addFlags(268435456);
                        CallOverlayService.this.startActivity(intent);
                    } catch (Exception unused) {
                    }
                    CallOverlayService.this.stopSelf();
                }
            });
            int overlayType = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -2, overlayType, 24, -3);
            layoutParams.gravity = 81;
            layoutParams.y = (int) (14.0f * getResources().getDisplayMetrics().density);
            layoutParams.width = getResources().getDisplayMetrics().widthPixels - ((int) (28.0f * getResources().getDisplayMetrics().density));
            this.windowManager.addView(this.overlayView, layoutParams);
        } catch (Exception unused2) {
            stopSelf();
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        if (this.overlayView != null) {
            try {
                this.windowManager.removeView(this.overlayView);
            } catch (Exception unused) {
            }
            this.overlayView = null;
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }
}
