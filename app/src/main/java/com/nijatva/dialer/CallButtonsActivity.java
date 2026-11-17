package com.nijatva.dialer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

/* Call button style chooser: round buttons or slide to answer */
public class CallButtonsActivity extends Activity {
    private SharedPreferences prefs;

    private void updateChecks() {
        boolean slide = "1".equals(this.prefs.getString("call_button_style", "0"));
        ((ImageView) findViewById(R.id.checkSlide)).setVisibility(slide ? 0 : 4);
        ((ImageView) findViewById(R.id.checkRound)).setVisibility(slide ? 4 : 0);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_call_buttons);
        this.prefs = getSharedPreferences("mydialer", 0);
        findViewById(R.id.callButtonsBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallButtonsActivity.this.finish();
            }
        });
        updateChecks();
        findViewById(R.id.cardSlide).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallButtonsActivity.this.prefs.edit().putString("call_button_style", "1").commit();
                CallButtonsActivity.this.updateChecks();
            }
        });
        findViewById(R.id.cardRound).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                CallButtonsActivity.this.prefs.edit().putString("call_button_style", "0").commit();
                CallButtonsActivity.this.updateChecks();
            }
        });
    }
    private int themeGen;

    @Override // android.app.ContextThemeWrapper
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
