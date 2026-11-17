package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/* Full-screen app lock: PIN keypad or pattern + forgot password recovery */
public class LockActivity extends Activity {
    private final StringBuffer pinBuffer = new StringBuffer();
    private TextView[] dots;
    private SharedPreferences prefs;
    private int themeGen;

    @Override // android.app.Activity
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_lock);
        this.prefs = getSharedPreferences("mydialer", 0);
        boolean hasPattern = this.prefs.getString("app_pattern", null) != null;
        findViewById(R.id.pinSection).setVisibility(hasPattern ? 8 : 0);
        findViewById(R.id.patternSection).setVisibility(hasPattern ? 0 : 8);
        ((TextView) findViewById(R.id.lockSubtitle)).setText(hasPattern ? R.string.lock_enter_pattern : R.string.lock_enter_pin);
        this.dots = new TextView[]{(TextView) findViewById(R.id.lockDot1), (TextView) findViewById(R.id.lockDot2), (TextView) findViewById(R.id.lockDot3), (TextView) findViewById(R.id.lockDot4)};
        updateDots();
        int[] keyIds = {R.id.lockKey1, R.id.lockKey2, R.id.lockKey3, R.id.lockKey4, R.id.lockKey5, R.id.lockKey6, R.id.lockKey7, R.id.lockKey8, R.id.lockKey9, R.id.lockKey0};
        for (int i = 0; i < keyIds.length; i++) {
            final TextView keyView = (TextView) findViewById(keyIds[i]);
            if (keyView != null) {
                keyView.setOnClickListener(new View.OnClickListener() {
                    @Override // android.view.View.OnClickListener
                    public void onClick(View view) {
                        LockActivity.this.onDigit(keyView.getText().toString());
                    }
                });
            }
        }
        View delKey = findViewById(R.id.lockKeyDel);
        if (delKey != null) {
            delKey.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    if (LockActivity.this.pinBuffer.length() > 0) {
                        LockActivity.this.pinBuffer.setLength(LockActivity.this.pinBuffer.length() - 1);
                        LockActivity.this.updateDots();
                    }
                }
            });
        }
        PatternView patternView = (PatternView) findViewById(R.id.patternView);
        patternView.setAccent(getResources().getColor(R.color.brand_green));
        patternView.setOnPatternListener(new PatternView.OnPatternListener() {
            @Override // com.nijatva.dialer.PatternView.OnPatternListener
            public void onPatternComplete(List<Integer> list) {
                if (list.size() < 4) {
                    Toast.makeText(LockActivity.this, R.string.lock_pattern_set, 0).show();
                    ((PatternView) LockActivity.this.findViewById(R.id.patternView)).clearPattern();
                    return;
                }
                LockActivity.this.verifyPattern(((PatternView) LockActivity.this.findViewById(R.id.patternView)).patternString());
            }
        });
        findViewById(R.id.lockForgot).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                LockActivity.this.showForgotDialog();
            }
        });
    }

    private void onDigit(String str) {
        if (this.pinBuffer.length() >= 4) {
            return;
        }
        this.pinBuffer.append(str);
        updateDots();
        if (this.pinBuffer.length() == 4) {
            verifyPin(this.pinBuffer.toString());
        }
    }

    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            if (i < this.pinBuffer.length()) {
                this.dots[i].setText("\u25cf");
                this.dots[i].setTextColor(getResources().getColor(R.color.brand_green));
            } else {
                this.dots[i].setText("\u25cb");
                this.dots[i].setTextColor(getResources().getColor(R.color.text_hint));
            }
        }
    }

    private void verifyPin(String str) {
        String stored = this.prefs.getString("app_pin", null);
        boolean ok;
        if (PinUtils.isHash(stored)) {
            String hash = PinUtils.sha256(this.prefs.getString("pin_salt", "") + str);
            ok = hash != null && hash.equals(stored);
        } else {
            ok = str.equals(stored);
            if (ok && stored != null) {
                this.prefs.edit().putString("app_pin", PinUtils.salted(this.prefs, str)).commit();
            }
        }
        if (ok) {
            setResult(-1);
            finish();
        } else {
            Toast.makeText(this, R.string.wrong_pin, 0).show();
            this.pinBuffer.setLength(0);
            updateDots();
        }
    }

    private void verifyPattern(String str) {
        String stored = this.prefs.getString("app_pattern", null);
        String hash = PinUtils.sha256(this.prefs.getString("pin_salt", "") + str);
        if (stored != null && hash != null && hash.equals(stored)) {
            setResult(-1);
            finish();
        } else {
            Toast.makeText(this, R.string.wrong_pin, 0).show();
            ((PatternView) findViewById(R.id.patternView)).clearPattern();
        }
    }

    private void showForgotDialog() {
        String q = this.prefs.getString("lock_question", "");
        if (q == null || q.trim().isEmpty()) {
            Toast.makeText(this, R.string.lock_recovery_not_set, 0).show();
            return;
        }
        final EditText answer = new EditText(this);
        answer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        answer.setHint(R.string.lock_answer_hint);
        new AlertDialog.Builder(this).setTitle(q).setView(answer).setPositiveButton(R.string.lock_reset, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                String stored = LockActivity.this.prefs.getString("lock_answer", null);
                String given = answer.getText().toString().trim().toLowerCase();
                boolean ok;
                if (PinUtils.isHash(stored)) {
                    String hash = PinUtils.sha256(LockActivity.this.prefs.getString("pin_salt", "") + given);
                    ok = hash != null && hash.equals(stored);
                } else {
                    ok = given.equals(stored);
                }
                if (ok) {
                    LockActivity.this.prefs.edit().remove("app_pin").remove("app_pattern").remove("lock_question").remove("lock_answer").remove("lock_type").commit();
                    Toast.makeText(LockActivity.this, R.string.lock_removed, 0).show();
                    LockActivity.this.setResult(-1);
                    LockActivity.this.finish();
                } else {
                    Toast.makeText(LockActivity.this, R.string.lock_wrong, 0).show();
                }
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
