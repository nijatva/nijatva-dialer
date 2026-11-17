package com.nijatva.dialer;

import android.content.Context;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;

public class FakeCallActivity extends Activity {

    private final Handler handler = new Handler();
    private long delayMs = 5000;
    private long answeredAt = 0;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            long s = (System.currentTimeMillis() - answeredAt) / 1000;
            ((TextView) findViewById(R.id.fakeTimer)).setText(String.format(Locale.US, "%02d:%02d", Long.valueOf(s / 60), Long.valueOf(s % 60)));
            if (findViewById(R.id.fakeActive).getVisibility() == View.VISIBLE) {
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);
        findViewById(R.id.fakeBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        setDelayListener(R.id.fakeDelay5, 5000);
        setDelayListener(R.id.fakeDelay10, 10000);
        setDelayListener(R.id.fakeDelay30, 30000);
        setDelayListener(R.id.fakeDelay60, 60000);
        findViewById(R.id.fakeStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((EditText) findViewById(R.id.fakeNameInput)).getText().toString().trim();
                String number = ((EditText) findViewById(R.id.fakeNumberInput)).getText().toString().trim();
                final String display = name.isEmpty() ? (number.isEmpty() ? "Unknown" : number) : name;
                findViewById(R.id.fakeConfig).setVisibility(View.GONE);
                TextView avatar = (TextView) findViewById(R.id.fakeAvatar);
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                d.setColor(0xFF1565C0);
                avatar.setBackground(d);
                avatar.setText(display.substring(0, 1).toUpperCase(Locale.US));
                ((TextView) findViewById(R.id.fakeCallerName)).setText(display);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.fakeRinging).setVisibility(View.VISIBLE);
                    }
                }, delayMs);
            }
        });
        findViewById(R.id.fakeAnswer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.fakeRinging).setVisibility(View.GONE);
                findViewById(R.id.fakeActive).setVisibility(View.VISIBLE);
                answeredAt = System.currentTimeMillis();
                handler.postDelayed(tickRunnable, 500);
            }
        });
        View.OnClickListener reset = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetAll();
            }
        };
        findViewById(R.id.fakeDecline).setOnClickListener(reset);
        findViewById(R.id.fakeEnd).setOnClickListener(reset);
    }

    private void resetAll() {
        handler.removeCallbacksAndMessages(null);
        findViewById(R.id.fakeRinging).setVisibility(View.GONE);
        findViewById(R.id.fakeActive).setVisibility(View.GONE);
        findViewById(R.id.fakeConfig).setVisibility(View.VISIBLE);
    }

    private void setDelayListener(final int id, final long ms) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delayMs = ms;
                int[] ids = {R.id.fakeDelay5, R.id.fakeDelay10, R.id.fakeDelay30, R.id.fakeDelay60};
                for (int i : ids) {
                    TextView t = (TextView) findViewById(i);
                    t.setBackgroundResource(i == id ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
                    t.setTextColor(i == id ? 0xFFFFFFFF : 0xFF9E9E9E);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
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

}
