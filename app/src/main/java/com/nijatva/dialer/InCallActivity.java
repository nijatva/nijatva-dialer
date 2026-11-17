package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

/* loaded from: classes.dex */
public class InCallActivity extends Activity {
    private static WeakReference<InCallActivity> instance;
    private ImageButton answerButton;
    private TextView answerLabel;
    private View answerSpace;
    private View slideWrap;
    private View slideThumb;
    private boolean slideMode;
    private AudioManager audioManager;
    private Call call;
    private CameraManager cameraManager;
    private View controlsRow;
    private ImageButton endButton;
    private TextView endLabel;
    private Handler flashHandler;
    private Runnable flashRunnable;
    private ImageButton holdButton;
    private ImageView incallAvatar;
    private ImageButton mergeButton;
    private ImageButton muteButton;
    private TextView numberView;
    private String resolvedName;
    private String resolvedFor;
    private ImageButton speakerButton;
    private ImageButton keypadButton;
    private View dtmfPad;
    private TextView mergeLabel;
    private Handler durHandler;
    private Runnable durRunnable;
    private long activeStart = -1L;
    private TextView stateView;
    private String torchCameraId;
    private boolean muted = false;
    private boolean speakerOn = false;
    private boolean torchOn = false;
    private int lastCallState = -1;
    private boolean missedEnded = false;
    private String currentNumber = "";
    private final Call.Callback callback = new Call.Callback() {
        @Override // android.telecom.Call.Callback
        public void onStateChanged(Call call, int i) {
            InCallActivity.this.runOnUiThread(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    InCallActivity.this.updateUi();
                }
            });
        }
    };


    public static void finishIfNoCalls() {
        WeakReference<InCallActivity> weakReference = instance;
        InCallActivity inCallActivity = weakReference == null ? null : weakReference.get();
        if (inCallActivity == null || !CallState.calls.isEmpty()) {
            return;
        }
        inCallActivity.returnToDialer();
    }

    private Bitmap getContactPhotoByNumber(String str) {
        InputStream openContactPhotoInputStream;
        if (str == null || str.isEmpty() || (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.READ_CONTACTS") != 0)) {
            return null;
        }
        try {
            Cursor query = getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)), new String[]{"_id"}, null, null, null);
            if (query != null) {
                Long valueOf = query.moveToFirst() ? Long.valueOf(query.getLong(0)) : null;
                query.close();
                if (valueOf == null || (openContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, valueOf.longValue()), true)) == null) {
                    return null;
                }
                Bitmap decodeStream = BitmapFactory.decodeStream(openContactPhotoInputStream);
                openContactPhotoInputStream.close();
                return decodeStream;
            }
        } catch (Exception unused) {
        }
        return null;
    }

    private void postMissedNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            String str = Tags.get(this, this.currentNumber);
            if (str == null) {
                str = this.currentNumber.isEmpty() ? getString(R.string.unknown_caller) : this.currentNumber;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                notificationManager.createNotificationChannel(new NotificationChannel("missed_calls", "Missed calls", 3));
            }
            int i = Build.VERSION.SDK_INT >= 23 ? 201326592 : 134217728;
            PendingIntent activity = PendingIntent.getActivity(this, 5, new Intent(this, (Class<?>) MainActivity.class), i);
            PendingIntent broadcast = PendingIntent.getBroadcast(this, 6, new Intent(this, (Class<?>) NotificationActionReceiver.class).setAction("com.nijatva.dialer.CALL_BACK"), i);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "missed_calls") : new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_call).setContentTitle(getString(R.string.missed_notif)).setContentText(str).setContentIntent(activity).setAutoCancel(true).setPriority(0);
            builder.addAction(R.drawable.ic_call, getString(R.string.call_back), broadcast);
            notificationManager.notify(2, builder.build());
        } catch (Exception unused) {
        }
    }


    public void rejectCall(Call call) {
        try {
            try {
                Call.class.getMethod("reject", new Class[0]).invoke(call, new Object[0]);
            } catch (NoSuchMethodException unused) {
                call.reject(false, null);
            }
        } catch (Throwable unused2) {
        }
    }

    private void returnToDialer() {
        try {
            CallState.justEnded = true;
            Intent intent = new Intent(this, (Class<?>) MainActivity.class);
            intent.addFlags(872415232);
            startActivity(intent);
        } catch (Exception unused) {
        }
        try {
            ((NotificationManager) getSystemService("notification")).cancel(1);
        } catch (Exception unused2) {
        }
        finish();
    }


    public void sendQuickSms(String str, String str2) {
        if (str != null && !str.isEmpty()) {
            try {
                try {
                    if (Build.VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.SEND_SMS") == 0) {
                        SmsManager.getDefault().sendTextMessage(str, null, str2, null, null);
                        Toast.makeText(this, R.string.qsms_sent, 0).show();
                        return;
                    }
                    requestPermissions(new String[]{"android.permission.SEND_SMS"}, 105);
                    try {
                        startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + str + "?body=" + Uri.encode(str2))));
                    } catch (Exception unused) {
                    }
                } catch (Exception unused2) {
                }
            } catch (Exception unused3) {
                startActivity(new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + str + "?body=" + Uri.encode(str2))));
            }
        }
    }


    public void showQuickSmsThenReject() {
        final String str = this.currentNumber;
        final String[] strArr = {getString(R.string.qsms1), getString(R.string.qsms2), getString(R.string.qsms3)};
        new AlertDialog.Builder(this).setTitle(R.string.quick_sms_title).setItems(strArr, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                InCallActivity.this.sendQuickSms(str, strArr[i]);
                InCallActivity inCallActivity = InCallActivity.this;
                inCallActivity.rejectCall(inCallActivity.call);
            }
        }).setNegativeButton(R.string.reject_only, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                InCallActivity inCallActivity = InCallActivity.this;
                inCallActivity.rejectCall(inCallActivity.call);
            }
        }).show();
    }

    private void startFlashBlink() {
        if (this.flashHandler == null && Build.VERSION.SDK_INT >= 23) {
            int i = 0;
            if (getSharedPreferences("mydialer", 0).getBoolean("flash_alert", true)) {
                try {
                    CameraManager cameraManager = (CameraManager) getSystemService("camera");
                    this.cameraManager = cameraManager;
                    if (this.torchCameraId == null) {
                        String[] cameraIdList = cameraManager.getCameraIdList();
                        int length = cameraIdList.length;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            String str = cameraIdList[i];
                            if (Boolean.TRUE.equals((Boolean) this.cameraManager.getCameraCharacteristics(str).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                                this.torchCameraId = str;
                                break;
                            }
                            i++;
                        }
                    }
                    final String str2 = this.torchCameraId;
                    if (str2 == null) {
                        return;
                    }
                    this.flashHandler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            try {
                                InCallActivity inCallActivity = InCallActivity.this;
                                inCallActivity.torchOn = !inCallActivity.torchOn;
                                InCallActivity.this.cameraManager.setTorchMode(str2, InCallActivity.this.torchOn);
                                if (InCallActivity.this.flashHandler != null) {
                                    InCallActivity.this.flashHandler.postDelayed(this, 350L);
                                }
                            } catch (Exception unused) {
                                InCallActivity.this.torchOn = false;
                            }
                        }
                    };
                    this.flashRunnable = runnable;
                    this.flashHandler.post(runnable);
                } catch (Exception unused) {
                    this.flashHandler = null;
                }
            }
        }
    }

    private void stopFlashBlink() {
        CameraManager cameraManager;
        String str;
        Handler handler = this.flashHandler;
        if (handler != null) {
            handler.removeCallbacks(this.flashRunnable);
            this.flashHandler = null;
        }
        if (this.torchOn && (cameraManager = this.cameraManager) != null && (str = this.torchCameraId) != null) {
            try {
                cameraManager.setTorchMode(str, false);
            } catch (Exception unused) {
            }
        }
        this.torchOn = false;
    }

    private void updateAvatar(String str) {
        try {
            Bitmap contactPhotoByNumber = getContactPhotoByNumber(str);
            if (contactPhotoByNumber != null) {
                this.incallAvatar.setImageBitmap(contactPhotoByNumber);
                this.incallAvatar.setVisibility(0);
            } else {
                this.incallAvatar.setVisibility(8);
            }
        } catch (Exception unused) {
        }
    }

    private void updateCallNotification(String str, String str2) {
        Notification.Builder builder;
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            Call call = this.call;
            if (call == null) {
                notificationManager.cancel(1);
                return;
            }
            int state = call.getState();
            if (state != 7 && !isFinishing()) {
                boolean z = state == 2;
                if (Build.VERSION.SDK_INT >= 26) {
                    NotificationChannel notificationChannel = new NotificationChannel("incoming_call", "Incoming call", 4);
                    notificationChannel.setShowBadge(true);
                    notificationManager.createNotificationChannel(notificationChannel);
                    NotificationChannel notificationChannel2 = new NotificationChannel("ongoing_call", "Ongoing call", 2);
                    notificationChannel2.setShowBadge(false);
                    notificationManager.createNotificationChannel(notificationChannel2);
                }
                int i = Build.VERSION.SDK_INT >= 23 ? 201326592 : 134217728;
                PendingIntent activity = PendingIntent.getActivity(this, 1, new Intent(this, (Class<?>) InCallActivity.class), i);
                PendingIntent broadcast = PendingIntent.getBroadcast(this, 2, new Intent(this, (Class<?>) NotificationActionReceiver.class).setAction("com.nijatva.dialer.ANSWER"), i);
                PendingIntent broadcast2 = PendingIntent.getBroadcast(this, 3, new Intent(this, (Class<?>) NotificationActionReceiver.class).setAction("com.nijatva.dialer.REJECT"), i);
                PendingIntent broadcast3 = PendingIntent.getBroadcast(this, 4, new Intent(this, (Class<?>) NotificationActionReceiver.class).setAction("com.nijatva.dialer.END"), i);
                if (Build.VERSION.SDK_INT >= 26) {
                    builder = new Notification.Builder(this, z ? "incoming_call" : "ongoing_call");
                } else {
                    builder = new Notification.Builder(this);
                }
                String str3 = Tags.get(this, str);
                if (str3 != null) {
                    str = str3;
                } else if (str.isEmpty()) {
                    str = getString(R.string.unknown_caller);
                }
                Notification.Builder contentTitle = builder.setSmallIcon(R.drawable.ic_call).setContentTitle(z ? str2 : getString(R.string.app_name));
                if (!z) {
                    str = str2 + "  •  " + str;
                }
                contentTitle.setContentText(str).setContentIntent(activity).setOngoing(true).setCategory("call").setPriority(z ? 1 : -1);
                if (z) {
                    builder.addAction(R.drawable.ic_call, getString(R.string.action_answer), broadcast);
                    builder.addAction(R.drawable.ic_call_end, getString(R.string.action_decline), broadcast2);
                } else {
                    builder.addAction(R.drawable.ic_call_end, getString(R.string.action_end), broadcast3);
                }
                notificationManager.notify(1, builder.build());
                return;
            }
            notificationManager.cancel(1);
        } catch (Exception unused) {
        }
    }



    public String findDisplayName(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            String dg = str.replaceAll("[^0-9]", "");
            if (dg.length() < 5) {
                return null;
            }
            android.database.Cursor query = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "data1"}, null, null, null);
            if (query != null) {
                while (query.moveToNext()) {
                    String num = query.getString(1);
                    if (num != null && num.replaceAll("[^0-9]", "").equals(dg)) {
                        String nm = query.getString(0);
                        query.close();
                        return nm;
                    }
                }
                query.close();
            }
        } catch (Exception unused) {
        }
        return null;
    }

    public void updateUi() {
        if (this.call == null || isFinishing()) {
            return;
        }
        String str = "";
        String schemeSpecificPart = (this.call.getDetails() == null || this.call.getDetails().getHandle() == null) ? "" : this.call.getDetails().getHandle().getSchemeSpecificPart();
        String str2 = Tags.get(this, schemeSpecificPart);
        String incEmoji = EmojiUtil.display(this, schemeSpecificPart);
        if (str2 == null) {
            if (!schemeSpecificPart.equals(this.resolvedFor)) {
                this.resolvedFor = schemeSpecificPart;
                this.resolvedName = findDisplayName(schemeSpecificPart);
            }
            str2 = this.resolvedName;
        }
        android.widget.TextView incallSub = (android.widget.TextView) findViewById(R.id.incallSub);
        if (str2 != null) {
            this.numberView.setText(incEmoji.isEmpty() ? str2 : incEmoji + " " + str2);
            if (incallSub != null) {
                incallSub.setText(PrivacyUtil.mask(this, schemeSpecificPart));
                incallSub.setVisibility(0);
            }
        } else {
            this.numberView.setText(schemeSpecificPart.isEmpty() ? getString(R.string.unknown_caller) : PrivacyUtil.mask(this, schemeSpecificPart));
            if (incallSub != null) {
                incallSub.setVisibility(8);
            }
        }
        String catIc = SpamUtil.getCategory(this, schemeSpecificPart);
        if (catIc == null) {
            catIc = SpamUtil.getSuspected(this, schemeSpecificPart);
        }
        if (catIc != null) {
            android.widget.TextView incallSubSpam = (android.widget.TextView) findViewById(R.id.incallSub);
            if (incallSubSpam != null) {
                incallSubSpam.setVisibility(0);
                incallSubSpam.setText((incallSubSpam.getText() + "  |  " + catIc).toString());
                incallSubSpam.setTextColor(SpamUtil.isSpam(this, schemeSpecificPart) ? 0xffff5252 : 0xffff9800);
            }
        }
        this.currentNumber = schemeSpecificPart;
        updateAvatar(schemeSpecificPart);
        ImageButton imageButton = this.mergeButton;
        boolean z = false;
        if (imageButton != null) {
            imageButton.setVisibility(CallState.calls.size() >= 2 ? 0 : 8);
        }
        if (this.mergeLabel != null) {
            this.mergeLabel.setVisibility(CallState.calls.size() >= 2 ? 0 : 8);
        }
        int state = this.call.getState();
        boolean z2 = state == 2;
        if (state == 2) {
            str = getString(R.string.state_incoming);
        } else if (state == 1 || state == 9) {
            str = getString(R.string.state_calling);
        } else if (state == 4) {
            str = getString(R.string.state_active);
        } else if (state == 3) {
            str = getString(R.string.state_holding);
        } else if (state == 7) {
            str = getString(R.string.state_ended);
        }
        this.stateView.setText(str);
        if (state == 4) {
            if (this.activeStart < 0L) {
                this.activeStart = SystemClock.elapsedRealtime();
            }
            startDurationTimer();
        } else {
            this.activeStart = -1L;
            stopDurationTimer();
            if (this.dtmfPad != null) {
                this.dtmfPad.setVisibility(8);
            }
        }
        updateCallNotification(schemeSpecificPart, str);
        int i = (z2 && !this.slideMode) ? 0 : 8;
        int i2 = z2 ? 8 : 0;
        if (this.slideWrap != null) {
            this.slideWrap.setVisibility((z2 && this.slideMode) ? 0 : 8);
            this.slideThumb.setTranslationX(0.0f);
        }
        this.answerButton.setVisibility(i);
        this.answerLabel.setVisibility(i);
        this.answerSpace.setVisibility(i);
        this.controlsRow.setVisibility(i2);
        this.endLabel.setText(getString(z2 ? R.string.action_decline : R.string.action_end));
        if (this.speakerButton.getBackground() != null) {
            this.speakerButton.getBackground().mutate().setColorFilter(this.speakerOn ? getSharedPreferences("mydialer", 0).getInt("accent", -15374912) : -14013910, PorterDuff.Mode.SRC_IN);
            this.speakerButton.invalidate();
        }
        if (this.muteButton.getBackground() != null) {
            this.muteButton.getBackground().mutate().setColorFilter(this.muted ? getSharedPreferences("mydialer", 0).getInt("accent", -15374912) : -14013910, PorterDuff.Mode.SRC_IN);
            this.muteButton.invalidate();
        }
        if (this.holdButton.getBackground() != null) {
            this.holdButton.getBackground().mutate().setColorFilter(state == 3 ? getSharedPreferences("mydialer", 0).getInt("accent", -15374912) : -14013910, PorterDuff.Mode.SRC_IN);
            this.holdButton.invalidate();
        }
        if (state == 2) {
            startFlashBlink();
        } else {
            stopFlashBlink();
        }
        int i3 = this.lastCallState;
        if (state != i3) {
            boolean z3 = i3 == 2 && state == 4;
            boolean z4 = (state != 7 || i3 == -1 || i3 == 7) ? false : true;
            if (state == 7 && i3 == 2) {
                z = true;
            }
            this.missedEnded = z;
            if ((z3 || z4) && !z2) {
                vibrateShort();
            }
            this.lastCallState = state;
        }
        if (state == 7) {
            CallState.lastCallNumber = this.currentNumber;
            CallState.lastCallMissed = this.missedEnded;
            if (this.missedEnded) {
                postMissedNotification();
            }
            returnToDialer();
        }
    }

    private void vibrateShort() {
        if (getSharedPreferences("mydialer", 0).getBoolean("vibrate_answer", false)) {
            try {
                Vibrator vibrator = (Vibrator) getSystemService("vibrator");
                if (vibrator == null || !vibrator.hasVibrator()) {
                    return;
                }
                vibrator.vibrate(80L);
            } catch (Exception unused) {
            }
        }
    }

    private String fmtDuration(long secs) {
        return String.format(Locale.US, "%02d:%02d", Long.valueOf(secs / 60L), Long.valueOf(secs % 60L));
    }

    private void startDurationTimer() {
        if (this.durHandler == null) {
            this.durHandler = new Handler();
            this.durRunnable = new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    if (InCallActivity.this.activeStart >= 0L && !InCallActivity.this.isFinishing()) {
                        InCallActivity.this.stateView.setText(InCallActivity.this.fmtDuration((SystemClock.elapsedRealtime() - InCallActivity.this.activeStart) / 1000L));
                        if (InCallActivity.this.durHandler != null) {
                            InCallActivity.this.durHandler.postDelayed(this, 500L);
                        }
                    }
                }
            };
            this.durHandler.post(this.durRunnable);
        }
    }

    private void stopDurationTimer() {
        Handler handler = this.durHandler;
        if (handler != null) {
            handler.removeCallbacks(this.durRunnable);
            this.durHandler = null;
            this.durRunnable = null;
        }
    }

    private void hookupDtmf(View view) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                hookupDtmf(viewGroup.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            final TextView textView = (TextView) view;
            String trim = textView.getText().toString().trim();
            if (trim.isEmpty()) {
                return;
            }
            final char c = trim.charAt(0);
            textView.setOnTouchListener(new View.OnTouchListener() {
                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view2, android.view.MotionEvent motionEvent) {
                    try {
                        if (InCallActivity.this.call == null) {
                            return true;
                        }
                        int action = motionEvent.getAction();
                        if (action == 0) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                InCallActivity.this.call.playDtmfTone(c);
                            }
                        } else if (action == 1 || action == 3) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                InCallActivity.this.call.stopDtmfTone();
                            }
                        }
                    } catch (Exception unused) {
                    }
                    return true;
                }
            });
        }
    }

    private int themeGen;

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_incall);
        instance = new WeakReference<>(this);
        this.audioManager = (AudioManager) getSystemService("audio");
        this.numberView = (TextView) findViewById(R.id.incallNumber);
        this.stateView = (TextView) findViewById(R.id.incallState);
        this.answerButton = (ImageButton) findViewById(R.id.answerButton);
        this.speakerButton = (ImageButton) findViewById(R.id.speakerButton);
        this.muteButton = (ImageButton) findViewById(R.id.muteButton);
        this.holdButton = (ImageButton) findViewById(R.id.holdButton);
        ImageButton imageButton = (ImageButton) findViewById(R.id.endButton);
        this.endButton = imageButton;
        try {
            imageButton.getBackground().mutate().setTint(0xffff5252);
        } catch (Exception unused) {
        }
        this.answerLabel = (TextView) findViewById(R.id.answerLabel);
        this.endLabel = (TextView) findViewById(R.id.endLabel);
        this.controlsRow = findViewById(R.id.controlsRow);
        this.answerSpace = findViewById(R.id.answerSpace);
        this.slideMode = "1".equals(getSharedPreferences("mydialer", 0).getString("call_button_style", "0"));
        this.slideWrap = findViewById(R.id.slideWrap);
        this.slideThumb = findViewById(R.id.slideThumb);
        this.keypadButton = (ImageButton) findViewById(R.id.keypadButton);
        this.dtmfPad = findViewById(R.id.dtmfPad);
        this.mergeLabel = (TextView) findViewById(R.id.mergeLabel);
        if (this.dtmfPad != null) {
            hookupDtmf(this.dtmfPad);
        }
        if (this.keypadButton != null) {
            this.keypadButton.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    if (InCallActivity.this.dtmfPad != null) {
                        InCallActivity.this.dtmfPad.setVisibility(InCallActivity.this.dtmfPad.getVisibility() == 0 ? 8 : 0);
                    }
                }
            });
        }
        ImageView imageView = (ImageView) findViewById(R.id.incallAvatar);
        this.incallAvatar = imageView;
        try {
            imageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override // android.view.ViewOutlineProvider
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            this.incallAvatar.setClipToOutline(true);
        } catch (Exception unused2) {
        }
        ImageButton imageButton2 = (ImageButton) findViewById(R.id.mergeButton);
        this.mergeButton = imageButton2;
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    if (CallState.calls.size() >= 2) {
                        InCallActivity.this.call.conference(CallState.calls.get(1));
                    }
                } catch (Exception unused3) {
                }
            }
        });
        Call primary = CallState.primary();
        this.call = primary;
        if (primary == null) {
            finish();
            return;
        }
        primary.registerCallback(this.callback);
        this.answerButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    InCallActivity.this.call.answer(0);
                    if (InCallActivity.this.getSharedPreferences("mydialer", 0).getBoolean("speaker_default", false)) {
                        InCallActivity.this.speakerOn = true;
                        InCallActivity.this.audioManager.setSpeakerphoneOn(true);
                        InCallActivity.this.updateUi();
                    }
                } catch (Exception unused3) {
                }
            }
        });
        if (this.slideThumb != null) {
            this.slideThumb.setOnTouchListener(new View.OnTouchListener() {
                private float downX = 0.0f;

                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
                    int action = motionEvent.getAction();
                    if (action == 0) {
                        this.downX = motionEvent.getRawX();
                        return true;
                    }
                    if (action == 2) {
                        float max = (float) ((InCallActivity.this.slideWrap.getWidth() - view.getWidth()) - ((int) ((8.0f * getResources().getDisplayMetrics().density))));
                        float dx = motionEvent.getRawX() - this.downX;
                        if (dx < 0.0f) {
                            dx = 0.0f;
                        }
                        if (dx > max) {
                            dx = max;
                        }
                        view.setTranslationX(dx);
                        return true;
                    }
                    if (action == 1 || action == 3) {
                        float max2 = (float) ((InCallActivity.this.slideWrap.getWidth() - view.getWidth()) - ((int) ((8.0f * getResources().getDisplayMetrics().density))));
                        if (view.getTranslationX() >= max2 * 0.6f) {
                            view.setTranslationX(0.0f);
                            try {
                                InCallActivity.this.call.answer(0);
                                if (InCallActivity.this.getSharedPreferences("mydialer", 0).getBoolean("speaker_default", false)) {
                                    InCallActivity.this.speakerOn = true;
                                    InCallActivity.this.audioManager.setSpeakerphoneOn(true);
                                    InCallActivity.this.updateUi();
                                }
                            } catch (Exception unused4) {
                            }
                        } else {
                            view.setTranslationX(0.0f);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
        this.endButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    if (InCallActivity.this.call.getState() == 2) {
                        InCallActivity.this.showQuickSmsThenReject();
                    } else {
                        InCallActivity.this.call.disconnect();
                    }
                } catch (Exception unused3) {
                }
            }
        });
        this.speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                InCallActivity.this.speakerOn = !InCallActivity.this.speakerOn;
                InCallActivity.this.audioManager.setSpeakerphoneOn(InCallActivity.this.speakerOn);
                InCallActivity.this.updateUi();
            }
        });
        this.muteButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                InCallActivity.this.muted = !InCallActivity.this.muted;
                try {
                    InCallActivity.this.audioManager.setMicrophoneMute(InCallActivity.this.muted);
                } catch (Exception unused3) {
                }
                InCallActivity.this.updateUi();
            }
        });
        this.holdButton.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    if (InCallActivity.this.call.getState() == 3) {
                        InCallActivity.this.call.unhold();
                    } else {
                        InCallActivity.this.call.hold();
                    }
                } catch (Exception unused3) {
                }
                InCallActivity.this.updateUi();
            }
        });
        updateUi();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23 && this.call != null && this.currentNumber != null) {
                android.telecom.PhoneAccountHandle ph = this.call.getDetails().getAccountHandle();
                android.telecom.TelecomManager tm = (android.telecom.TelecomManager) getSystemService("telecom");
                if (ph != null && tm != null) {
                    java.util.List<android.telecom.PhoneAccountHandle> phs = tm.getCallCapablePhoneAccounts();
                    int slot = phs == null ? -1 : phs.indexOf(ph);
                    if (slot >= 0 && slot <= 1) {
                        String dg = SpamUtil.digits(this.currentNumber);
                        if (dg.length() >= 7) {
                            java.util.Set<String> old = getSharedPreferences("mydialer", 0).getStringSet("sim_hint", null);
                            java.util.HashSet<String> set = old == null ? new java.util.HashSet<String>() : new java.util.HashSet<String>(old);
                            for (String s : new java.util.HashSet<String>(set)) {
                                if (s.startsWith(dg + "|")) {
                                    set.remove(s);
                                }
                            }
                            set.add(dg + "|" + slot);
                            getSharedPreferences("mydialer", 0).edit().putStringSet("sim_hint", set).commit();
                        }
                    }
                }
            }
        } catch (Exception unusedSim) {
        }
        stopDurationTimer();
        try {
            ((NotificationManager) getSystemService("notification")).cancel(1);
        } catch (Exception unused) {
        }
        stopFlashBlink();
        Call call = this.call;
        if (call != null) {
            try {
                call.unregisterCallback(this.callback);
            } catch (Exception unused2) {
            }
        }
        WeakReference<InCallActivity> weakReference = instance;
        if (weakReference != null && weakReference.get() == this) {
            instance = null;
        }
        AudioManager audioManager = this.audioManager;
        if (audioManager != null) {
            try {
                audioManager.setSpeakerphoneOn(false);
                this.audioManager.setMicrophoneMute(false);
            } catch (Exception unused3) {
            }
        }
    }
}
